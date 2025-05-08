package eu.dataspace.connector.tests;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Map;
import java.util.function.Function;

public interface SeedVault {

    static ServiceExtension fromMap(Function<ServiceExtensionContext, Map<String, String>> secretsProvider) {
        return new ServiceExtension() {

            @Inject
            private Vault vault;

            @Override
            public void initialize(ServiceExtensionContext context) {
                secretsProvider.apply(context).forEach(vault::storeSecret);
            }
        };
    }

}
