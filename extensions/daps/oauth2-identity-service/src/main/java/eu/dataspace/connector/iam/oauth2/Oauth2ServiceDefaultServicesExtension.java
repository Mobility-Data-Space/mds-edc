package eu.dataspace.connector.iam.oauth2;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Provides default service implementations for fallback
 */
public class Oauth2ServiceDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Core Default Services";

    @Override
    public String name() {
        return NAME;
    }


    @Provider(isDefault = true)
    public AudienceResolver defaultAudienceResolver() {
        return (msg) -> Result.success(msg.getCounterPartyAddress());
    }
}
