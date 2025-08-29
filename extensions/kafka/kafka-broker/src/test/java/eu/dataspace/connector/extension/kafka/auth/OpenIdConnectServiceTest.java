package eu.dataspace.connector.extension.kafka.auth;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import dev.failsafe.RetryPolicy;
import eu.dataspace.connector.extension.kafka.broker.openid.ClientRegistrationResponse;
import eu.dataspace.connector.extension.kafka.broker.openid.OpenIdConnectService;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

@Testcontainers
class OpenIdConnectServiceTest {

    @Container
    private final KeycloakContainer keycloak = new KeycloakContainer()
            .withLogConsumer(o -> System.out.println(o.getUtf8StringWithoutLineEnding()));
    private final JacksonTypeManager typeManager = new JacksonTypeManager();
    private final EdcHttpClientImpl edcHttpClient = new EdcHttpClientImpl(new OkHttpClient.Builder().build(), RetryPolicy.ofDefaults(), mock());

    private final OpenIdConnectService service = new OpenIdConnectService(edcHttpClient, typeManager.getMapper());

    @Test
    void shouldRegisterNewClient() {
        var adminClient = createAdminClient();
        var realmName = "kafka";
        createRealm(adminClient, realmName);
        var initialAccessToken = createInitialAccessToken(adminClient, realmName);

        var oidcDiscoveryUrl = "http://localhost:%s/realms/%s/.well-known/openid-configuration"
                .formatted(keycloak.getFirstMappedPort(), realmName);

        var registration = service.fetchOpenIdConfiguration(oidcDiscoveryUrl)
                .compose(configuration -> service.registerNewClient(configuration, initialAccessToken));

        assertThat(registration).isSucceeded().isNotNull().extracting(ClientRegistrationResponse::clientId).isNotNull();
    }

    @Nested
    class UserInfo {
        @Test
        void shouldRetrieveUserInfo() {
            var adminClient = createAdminClient();
            var realmName = "kafka";
            createRealm(adminClient, realmName);
            var initialAccessToken = createInitialAccessToken(adminClient, realmName);
            var oidcDiscoveryUrl = "http://localhost:%s/realms/%s/.well-known/openid-configuration"
                    .formatted(keycloak.getFirstMappedPort(), realmName);

            var result = service.fetchOpenIdConfiguration(oidcDiscoveryUrl)
                    .compose(openIdConfiguration -> service.registerNewClient(openIdConfiguration, initialAccessToken)
                            .compose(response -> service.userInfo(openIdConfiguration, response)));

            assertThat(result).isSucceeded().extracting(it -> it.sub()).isNotNull().satisfies(UUID::fromString);
        }
    }

    private String createInitialAccessToken(Keycloak adminClient, String realmName) {
        return adminClient.realm(realmName)
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(10, 100))
                .getToken();
    }

    private Keycloak createAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:%s".formatted(keycloak.getFirstMappedPort()))
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();
    }

    private void createRealm(Keycloak adminClient, String realmName) {
        var realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);

        adminClient.realms().create(realm);
        var realmResource = adminClient.realms().realm(realmName);

        var config = Map.of(
                "access.token.claim", "true",
                "id.token.claim", "true",
                "userinfo.token.claim", "true",
                "jsonType.label", "String",
                "multivalued", "true"
        );

        var mapper = new ProtocolMapperRepresentation();
        mapper.setName("scope_mapper");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
        mapper.setConfig(config);

        var clientScopeRepresentation = new ClientScopeRepresentation();
        clientScopeRepresentation.setId("openid");
        clientScopeRepresentation.setDescription("Adds scope claim with openid value");
        clientScopeRepresentation.setName("openid");
        clientScopeRepresentation.setProtocol("openid-connect");
        clientScopeRepresentation.setAttributes(Collections.singletonMap("include.in.token.scope", "true"));
        clientScopeRepresentation.setProtocolMappers(List.of(mapper));

        realmResource.clientScopes().create(clientScopeRepresentation);
        realmResource.addDefaultDefaultClientScope("openid");
    }

}
