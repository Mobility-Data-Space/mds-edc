package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.dataplane.kafka.spi.Credentials;
import eu.dataspace.connector.dataplane.kafka.spi.IdentityProvider;
import eu.dataspace.connector.extension.kafka.broker.openid.OpenIdConnectService;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_DISCOVERY_URL;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_REGISTER_CLIENT_TOKEN_KEY;

public class OpenIdConnectIdentityProvider implements IdentityProvider {

    private final OpenIdConnectService openIdConnectService;
    private final Vault vault;

    public OpenIdConnectIdentityProvider(OpenIdConnectService openIdConnectService, Vault vault) {
        this.openIdConnectService = openIdConnectService;
        this.vault = vault;
    }

    @Override
    public ServiceResult<Credentials> grantAccess(String dataFlowId, DataAddress dataAddress) {
        var discoveryUrl = dataAddress.getStringProperty(OIDC_DISCOVERY_URL);
        var tokenKey = dataAddress.getStringProperty(OIDC_REGISTER_CLIENT_TOKEN_KEY);
        var token = vault.resolveSecret(tokenKey);

        return openIdConnectService.fetchOpenIdConfiguration(discoveryUrl)
                .compose(configuration -> openIdConnectService.registerNewClient(configuration, token)
                        .compose(client -> openIdConnectService.userInfo(configuration, client)
                                .map(userInfo -> new Credentials(
                                        userInfo.sub(), configuration.tokenEndpoint(), client.clientId(), client.clientSecret()))));
    }

    @Override
    public ServiceResult<Void> revokeAccess(String dataFlowId) {
        return ServiceResult.success(); // TODO: client removal
    }
}
