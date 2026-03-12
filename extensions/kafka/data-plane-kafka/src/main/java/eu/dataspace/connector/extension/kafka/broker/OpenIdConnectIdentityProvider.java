package eu.dataspace.connector.extension.kafka.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.dataplane.kafka.spi.Credentials;
import eu.dataspace.connector.dataplane.kafka.spi.IdentityProvider;
import eu.dataspace.connector.extension.kafka.broker.openid.OpenIdConnectService;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_DISCOVERY_URL;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_REGISTER_CLIENT_TOKEN_KEY;

public class OpenIdConnectIdentityProvider implements IdentityProvider {

    private final OpenIdConnectService openIdConnectService;
    private final Vault vault;
    private final ObjectMapper objectMapper;

    public OpenIdConnectIdentityProvider(OpenIdConnectService openIdConnectService, Vault vault, ObjectMapper objectMapper) {
        this.openIdConnectService = openIdConnectService;
        this.vault = vault;
        this.objectMapper = objectMapper;
    }

    @Override
    public ServiceResult<Credentials> grantAccess(String dataFlowId, DataAddress dataAddress) {
        var discoveryUrl = dataAddress.getStringProperty(OIDC_DISCOVERY_URL);
        var tokenKey = dataAddress.getStringProperty(OIDC_REGISTER_CLIENT_TOKEN_KEY);
        var token = vault.resolveSecret(tokenKey);

        return openIdConnectService.fetchOpenIdConfiguration(discoveryUrl)
                .compose(configuration -> openIdConnectService.registerNewClient(configuration, token)
                        .compose(client -> {
                            storeClientInfo(dataFlowId, new ClientInfo(client.registrationClientUri(), client.registrationAccessToken()));
                            return openIdConnectService.userInfo(configuration, client)
                                    .map(userInfo -> new Credentials(
                                            userInfo.sub(), configuration.tokenEndpoint(), client.clientId(), client.clientSecret()));
                        }));
    }

    @Override
    public ServiceResult<Void> revokeAccess(String dataFlowId) {
        var clientInfo = fetchClientInfo(dataFlowId);
        if (clientInfo == null) {
            return ServiceResult.success();
        }
        return openIdConnectService.deleteClient(clientInfo.registrationClientUri(), clientInfo.registrationAccessToken())
                .onSuccess(it -> cleanupClientInfo(dataFlowId));
    }

    private ClientInfo fetchClientInfo(String dataFlowId) {
        var secretKey = clientInfoSecretKey(dataFlowId);
        var json = vault.resolveSecret(secretKey);
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, ClientInfo.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void cleanupClientInfo(String dataFlowId) {
        vault.deleteSecret(clientInfoSecretKey(dataFlowId));
    }

    private void storeClientInfo(String dataFlowId, ClientInfo clientInfo) {
        try {
            var json = objectMapper.writeValueAsString(clientInfo);
            vault.storeSecret(clientInfoSecretKey(dataFlowId), json);
        } catch (JsonProcessingException ignored) {

        }
    }

    private @NotNull String clientInfoSecretKey(String dataFlowId) {
        return "data-flow-" + dataFlowId + "-openid-connect-client-info";
    }

    private record ClientInfo(String registrationClientUri, String registrationAccessToken) {}
}
