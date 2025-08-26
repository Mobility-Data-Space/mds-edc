package eu.dataspace.connector.iam.daps;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenDecoratorRegistry;

/**
 * Provides specialization of Oauth2 extension to interact with DAPS instance
 */
@Extension(value = DapsExtension.NAME)
public class DapsExtension implements ServiceExtension {

    public static final String NAME = "DAPS";
    public static final String DEFAULT_TOKEN_SCOPE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";
    public static final String OAUTH_2_DAPS_TOKEN_CONTEXT = "oauth2-daps";

    @Setting(description = "The value of the scope claim that is passed to DAPS to obtain a DAT", defaultValue = DEFAULT_TOKEN_SCOPE, key = "edc.iam.token.scope")
    private String scope;
    
    @Inject
    private TokenDecoratorRegistry jwtDecoratorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.getMonitor().warning("The extension %s has been deprecated, please switch to a decentralized implementation".formatted(NAME));
        jwtDecoratorRegistry.register(OAUTH_2_DAPS_TOKEN_CONTEXT, new DapsJwtDecorator());
    }

    @Provider
    public TokenDecorator createDapsTokenDecorator(ServiceExtensionContext context) {
        return new DapsTokenDecorator(scope);
    }
}
