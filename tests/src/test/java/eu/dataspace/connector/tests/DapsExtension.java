package eu.dataspace.connector.tests;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.SystemExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import static eu.dataspace.connector.tests.Crypto.encode;
import static eu.dataspace.connector.tests.Crypto.generateKeyPair;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class DapsExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String JWKS_PATH = "/.well-known/jwks.json";

    private final GenericContainer<?> daps = new GenericContainer<>("ghcr.io/fraunhofer-aisec/omejdn-server:1.4.2")
            .withExposedPorts(4567)
            .withCopyFileToContainer(forClasspathResource("daps"), "/opt")
            .withCopyToContainer(Transferable.of(encode(generateKeyPair().getPrivate())), "/opt/keys/signing_key.pem")
            .withLogConsumer(f -> System.out.println(f.getUtf8StringWithoutLineEnding()))
            .waitingFor(Wait.forHttp(JWKS_PATH));

    @Override
    public void beforeAll(ExtensionContext context) {
        for (var client : Client.values()) {
            daps.withCopyToContainer(Transferable.of(client.encodedCertificate()), client.certificatePath());
        }
        daps.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        daps.stop();
    }

    public Config dapsConfig(String participantId) {
        var dapsUrl = "http://%s:%s".formatted(daps.getHost(), daps.getFirstMappedPort());

        var settings = Map.of(
                "edc.oauth.token.url", dapsUrl + "/token",
                "edc.oauth.client.id", participantId,
                "edc.oauth.private.key.alias", "daps-private-key",
                "edc.oauth.certificate.alias", "daps-certificate",
                "edc.oauth.provider.jwks.url", dapsUrl + "/.well-known/jwks.json",
                "edc.iam.token.scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL",
                "edc.oauth.provider.audience", "idsc:IDS_CONNECTORS_ALL",
                "edc.oauth.endpoint.audience", "idsc:IDS_CONNECTORS_ALL"
        );

        return ConfigFactory.fromMap(settings);
    }



    public SystemExtension seedExtension() {
        return new ServiceExtension() {

            @Inject
            private Vault vault;

            @Override
            public void initialize(ServiceExtensionContext context) {
                var client = Client.valueOf(context.getParticipantId());
                vault.storeSecret("daps-private-key", client.encodedPrivateKey());
                vault.storeSecret("daps-certificate", client.encodedCertificate());
            }

        };
    }

    enum Client {
        provider, consumer;

        private final X509Certificate certificate;
        private final PrivateKey privateKey;

        Client() {
            var keyPair = generateKeyPair();
            this.certificate = Crypto.createCertificate(keyPair);
            this.privateKey = keyPair.getPrivate();
        }

        public String certificatePath() {
            return "/opt/keys/%s.cert".formatted(Base64.getEncoder().encodeToString(name().getBytes()));
        }

        public String encodedCertificate() {
            return encode(certificate);
        }

        public String encodedPrivateKey() {
            return encode(privateKey);
        }

    }

}
