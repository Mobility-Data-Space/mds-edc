package eu.dataspace.connector.tests.feature;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.S3Extension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class S3DataSinkTest {

    @Nested
    class Daps extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer");

        @RegisterExtension
        @Order(2)
        private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

        @RegisterExtension
        @Order(3)
        private static final S3Extension CONSUMER_S3 = new S3Extension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected Daps() {
            super(PROVIDER, CONSUMER, CONSUMER_S3);
        }
    }

    @Nested
    class Dcp extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(0)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("issuer", "wallet", "consumer", "provider");

        @RegisterExtension
        @Order(1)
        private static final IssuerExtension ISSUER = new IssuerExtension(POSTGRES_EXTENSION, VAULT_EXTENSION);

        @RegisterExtension
        @Order(2)
        private static final Wallet IDENTITY_HUB = MdsParticipantFactory.wallet(POSTGRES_EXTENSION, VAULT_EXTENSION, "consumer", "provider");

        @RegisterExtension
        @Order(3)
        private static final S3Extension CONSUMER_S3 = new S3Extension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVaultDcp("consumer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        protected Dcp() {
            super(PROVIDER, CONSUMER, CONSUMER_S3);
        }

        @BeforeAll
        static void setUp() {
            ISSUER.registerAttestationAndCredentialDefinition();
            ISSUER.registerHolder(PROVIDER.getId(), PROVIDER.getName());
            ISSUER.registerHolder(CONSUMER.getId(), CONSUMER.getName());
            IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
            IDENTITY_HUB.requestCredentialIssuance(CONSUMER.getId(), ISSUER.did().get());
        }
    }

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;
        private final S3Extension consumerS3;

        protected Tests(MdsParticipant provider, MdsParticipant consumer, S3Extension consumerS3) {
            this.provider = provider;
            this.consumer = consumer;
            this.consumerS3 = consumerS3;
        }

        @Test
        void shouldSupportS3DataSinkTransfer() {
            var providerDataSource = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            providerDataSource.start();
            providerDataSource.stubFor(get(urlPathEqualTo("/source")).willReturn(ok("data")));

            var bucketName = UUID.randomUUID().toString();

            var userAccessKey = consumerS3.createBucket(bucketName);

        consumer.getService(Vault.class).storeSecret("s3credentials", Json.createObjectBuilder()
                .add("edctype", "dataspaceconnector:secrettoken")
                .add("accessKeyId", userAccessKey.accessKeyId())
                .add("secretAccessKey", userAccessKey.secretAccessKey())
                .build().toString());

            Map<String, Object> dataAddressProperties = Map.of(
                    EDC_NAMESPACE + "type", "HttpData",
                    EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(providerDataSource.port())
            );

            var assetId = provider.createOffer(dataAddressProperties);
            var objectName = UUID.randomUUID().toString();

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PUSH")
                    .withDestination(createObjectBuilder()
                            .add(TYPE, EDC_NAMESPACE + "DataAddress")
                            .add(EDC_NAMESPACE + "keyName", "s3credentials")
                            .add(EDC_NAMESPACE + "type", "AmazonS3")
                            .add(EDC_NAMESPACE + "endpointOverride", consumerS3.getEndpoint().toString())
                            .add(EDC_NAMESPACE + "region", "eu-central-1")
                            .add(EDC_NAMESPACE + "bucketName", bucketName)
                            .add(EDC_NAMESPACE + "objectName", objectName)
                            .build())
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, COMPLETED);

            await().untilAsserted(() -> {
                var body = consumerS3.getS3Client().getObject(b -> b.bucket(bucketName).key(objectName)).readAllBytes();
                assertThat(body).asString().isEqualTo("data");
            });

            providerDataSource.stop();
        }
    }

}
