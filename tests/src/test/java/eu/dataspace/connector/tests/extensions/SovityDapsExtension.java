package eu.dataspace.connector.tests.extensions;

import eu.dataspace.connector.tests.Crypto;
import eu.dataspace.connector.tests.SeedVault;
import org.eclipse.edc.spi.system.SystemExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.containers.GenericContainer;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static eu.dataspace.connector.tests.Crypto.encode;
import static eu.dataspace.connector.tests.Crypto.generateKeyPair;

public class SovityDapsExtension implements BeforeAllCallback, AfterAllCallback {

    private final static String REALM_NAME = "daps";
    private final GenericContainer<?> daps = new GenericContainer<>("ghcr.io/sovity/sovity-daps:26.0.7-1")
            .withExposedPorts(8080)
            .withCommand("start-dev")
            .withEnv("KC_HOSTNAME", "localhost")
            .withEnv("KC_HTTP_ENABLED", "true")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "password")
            .withEnv("TZ", ZoneId.systemDefault().toString())

            .withEnv("KC_LOG_LEVEL", "INFO")
            .withEnv("KC_LOG_LEVEL_ORG_KEYCLOAK", "DEBUG")
            .withEnv("KC_LOG_LEVEL_ORG_KEYCLOAK_PROTOCOL_OIDC", "TRACE")
            .withEnv("KC_LOG_LEVEL_ORG_KEYCLOAK_AUTHENTICATION", "DEBUG")

            .withEnv("KC_LOG_LEVEL_ORG_KEYCLOAK_SERVICES", "TRACE")
            .withEnv("KC_LOG_LEVEL_ORG_KEYCLOAK_EVENTS", "DEBUG")
            .withEnv("KC_LOG_LEVEL_ORG_KEYCLOAK_AUTHENTICATION_AUTHENTICATORS_CLIENT", "TRACE")
            .withLogConsumer(o -> System.out.println(o.getUtf8StringWithoutLineEnding()));

    @Override
    public void beforeAll(ExtensionContext context) {
        daps.start();

        var keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:%s".formatted(daps.getFirstMappedPort()))
                .realm("master")
                .username("admin")
                .password("password")
                .clientId("admin-cli")
                .build();

        var realm = new RealmRepresentation();
        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);
        keycloak.realms().create(realm);

        var dapsRealm = keycloak.realm(REALM_NAME);

        var scopes = List.of("idsc:IDS_CONNECTOR_ATTRIBUTES_ALL");

        scopes.forEach(scope -> dapsRealm.clientScopes().create(createScope(scope)));

        var clientsResource = dapsRealm.clients();
        for (var client : Client.values()) {
            clientsResource.create(createClient(client));

            var internalClientId = clientsResource.findByClientId(client.name()).getFirst().getId();

            dapsRealm.clients().get(internalClientId)
                    .getProtocolMappers()
                    .createMapper(createDatMapper(client));
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        daps.stop();
    }

    public Config dapsConfig(String participantId) {
        var keycloakRealmUser = "http://localhost:%s/realms/%s".formatted(daps.getFirstMappedPort(), REALM_NAME);
        var dapsUrl = keycloakRealmUser + "/protocol/openid-connect";

        var settings = Map.of(
                "edc.oauth.token.url", dapsUrl + "/token",
                "edc.oauth.client.id", participantId,
                "edc.oauth.private.key.alias", "daps-private-key",
                "edc.oauth.certificate.alias", "daps-certificate",
                "edc.oauth.provider.jwks.url", dapsUrl + "/certs",
                "edc.oauth.token.expiration", "60",
                "edc.iam.token.scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL",
                "edc.oauth.provider.audience", keycloakRealmUser,
                "edc.oauth.endpoint.audience", "idsc:IDS_CONNECTORS_ALL"
        );

        return ConfigFactory.fromMap(settings);
    }

    public SystemExtension seedDapsKeyPair() {
        return SeedVault.fromMap(context -> {
            var client = Client.valueOf(context.getParticipantId());
            return Map.of(
                    "daps-private-key", client.encodedPrivateKey(),
                    "daps-certificate", client.encodedCertificate()
            );
        });
    }

    private @NotNull ClientScopeRepresentation createScope(String name) {
        var scope = new ClientScopeRepresentation();
        scope.setName(name);
        scope.setProtocol("openid-connect");
        scope.setAttributes(Map.of("include.in.token.scope", "true"));
        return scope;
    }

    private @NotNull ClientRepresentation createClient(Client client) {
        var clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId(client.name());
        clientRepresentation.setStandardFlowEnabled(false);
        clientRepresentation.setDirectAccessGrantsEnabled(false);
        clientRepresentation.setServiceAccountsEnabled(true);
        clientRepresentation.setClientAuthenticatorType("client-jwt");
        clientRepresentation.setDefaultClientScopes(List.of("idsc:IDS_CONNECTOR_ATTRIBUTES_ALL"));
        clientRepresentation.setAttributes(Map.of(
                "access.token.signed.response.alg", "RS256",
                "use.jwks.url", "false",
                "jwt.credential.client.auth.only", "true",
                "jwt.credential.certificate", client.encodedCertificate()
        ));
        return clientRepresentation;
    }

    private ProtocolMapperRepresentation createDatMapper(Client client) {
        var datMapper = new ProtocolMapperRepresentation();
        datMapper.setProtocol("openid-connect");
        datMapper.setProtocolMapper("dat-mapper");
        datMapper.setName("DAT mapper");
        datMapper.setConfig(Map.of(
                "security-profile-claim", "idsc:BASE_SECURITY_PROFILE",
                "audience-claim", "idsc:IDS_CONNECTORS_ALL",
                "scope-claim", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL",
                "subject-claim", client.name(),
                "referring-connector-claim", client.name(),
                "access.token.claim", "true"
        ));
        return datMapper;
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

        public String encodedCertificate() {
            return encode(certificate);
        }

        public String encodedPrivateKey() {
            return encode(privateKey);
        }

    }

}
