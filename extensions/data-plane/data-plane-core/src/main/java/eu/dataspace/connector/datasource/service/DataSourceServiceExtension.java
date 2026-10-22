package eu.dataspace.connector.datasource.service;

import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static eu.dataspace.connector.datasource.service.DataSourceServiceExtension.NAME;

@Extension(NAME)
public class DataSourceServiceExtension implements ServiceExtension {

    static final String NAME = "Data Source Service Extension";

    @Inject
    private DataSourceStore store;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataSourceService createDataSourceService() {
        return new DataSourceServiceImpl(store, transactionContext);
    }
}
