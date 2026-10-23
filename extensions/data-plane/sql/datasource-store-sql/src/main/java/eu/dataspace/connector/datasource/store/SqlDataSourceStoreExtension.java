package eu.dataspace.connector.datasource.store;

import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.store.sql.PostgresDataSourceStatements;
import eu.dataspace.connector.datasource.store.sql.SqlDataSourceStatements;
import eu.dataspace.connector.datasource.store.sql.SqlDataSourceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

@Extension(value = SqlDataSourceStoreExtension.NAME)
public class SqlDataSourceStoreExtension implements ServiceExtension {

    protected static final String NAME = "SQL Data Source Store";

    @Setting(key = "edc.sql.store.datasource.datasource",
            description = "Datasource name for the SQL Data Source store",
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

    @Inject
    private Vault vault;

    @Inject
    private Clock clock;

    @Inject(required = false)
    private SqlDataSourceStatements statements;

    @Provider
    public DataSourceStore sqlStore() {
        return new SqlDataSourceStore(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), queryExecutor, getStatements(), vault, clock);
    }

    @Override
    public String name() {
        return NAME;
    }

    private SqlDataSourceStatements getStatements() {
        return statements == null ? new PostgresDataSourceStatements() : statements;
    }
}
