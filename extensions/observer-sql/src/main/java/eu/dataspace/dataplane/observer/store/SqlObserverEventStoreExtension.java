package eu.dataspace.dataplane.observer.store;

import eu.dataspace.dataplane.observer.store.sql.ObserverEventStatements;
import eu.dataspace.dataplane.observer.store.sql.SqlObserverEventStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Extension(value = SqlObserverEventStoreExtension.NAME)
public class SqlObserverEventStoreExtension implements ServiceExtension {

    static final String NAME = "SQL Observer Event Store";

    @Setting(key = "edc.sql.store.observerevent.datasource",
            description = "Datasource name for the SQL observer event store",
            defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ObserverEventStore sqlStore() {
        return new SqlObserverEventStore(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), queryExecutor, new ObserverEventStatements());
    }
}
