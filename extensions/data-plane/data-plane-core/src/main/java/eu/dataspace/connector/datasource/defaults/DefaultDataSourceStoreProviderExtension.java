package eu.dataspace.connector.datasource.defaults;

import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

import static eu.dataspace.connector.datasource.defaults.DefaultDataSourceStoreProviderExtension.NAME;

@Extension(NAME)
public class DefaultDataSourceStoreProviderExtension implements ServiceExtension {

    static final String NAME = "Default Data Source Store Provider";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;
    @Inject
    private Vault vault;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DataSourceStore createInMemoryStore() {
        return new InMemoryDataSourceStore(criterionOperatorRegistry, vault, typeManager.getMapper(), clock);
    }
}
