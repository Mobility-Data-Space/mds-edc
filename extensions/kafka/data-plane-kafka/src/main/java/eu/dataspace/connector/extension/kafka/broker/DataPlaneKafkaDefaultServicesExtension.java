package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.dataplane.kafka.spi.AccessControlLists;
import eu.dataspace.connector.dataplane.kafka.spi.IdentityProvider;
import eu.dataspace.connector.extension.kafka.broker.acls.KafkaAccessControlLists;
import eu.dataspace.connector.extension.kafka.broker.openid.OpenIdConnectService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

public class DataPlaneKafkaDefaultServicesExtension implements ServiceExtension {

    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Vault vault;

    @Provider(isDefault = true)
    public IdentityProvider identityProvider() {
        var openIdConnectService = new OpenIdConnectService(httpClient, typeManager.getMapper());
        return new OpenIdConnectIdentityProvider(openIdConnectService, vault);
    }

    @Provider(isDefault = true)
    public AccessControlLists accessControlLists() {
        return new KafkaAccessControlLists(vault);
    }
}
