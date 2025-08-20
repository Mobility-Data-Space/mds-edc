package eu.dataspace.connector.extension.kafka.auth;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import dev.failsafe.RetryPolicy;
import eu.dataspace.connector.extension.kafka.broker.auth.ClientRegistrationResponse;
import eu.dataspace.connector.extension.kafka.broker.auth.OpenIdConnectService;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

        var openIdConnectDiscoveryUrl = "http://localhost:%s/realms/%s/.well-known/openid-configuration"
                .formatted(keycloak.getFirstMappedPort(), realmName);

        var registration = service.registerNewClient(openIdConnectDiscoveryUrl, initialAccessToken);

        assertThat(registration).isSucceeded().isNotNull().extracting(ClientRegistrationResponse::clientId).isNotNull();
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
    }
}
