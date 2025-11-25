package eu.dataspace.connector.agreements.retirement.store;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.store.sql.PostgresAgreementRetirementStatements;
import eu.dataspace.connector.agreements.retirement.store.sql.SqlAgreementsRetirementStatements;
import eu.dataspace.connector.agreements.retirement.store.sql.SqlAgreementsRetirementStore;

@Extension(value = SqlAgreementsRetirementStoreExtension.NAME)
public class SqlAgreementsRetirementStoreExtension implements ServiceExtension {

    protected static final String NAME = "SQL Agreement Retirement Store.";

    @Setting(value = "Datasource name for the SQL AgreementsRetirement store", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    private static final String DATASOURCE_SETTING_NAME = "edc.sql.store.agreementretirement.datasource";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject(required = false)
    private SqlAgreementsRetirementStatements statements;

    @Provider
    public AgreementsRetirementStore sqlStore(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
        return new SqlAgreementsRetirementStore(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), queryExecutor, getStatements());
    }

    @Override
    public String name() {
        return NAME;
    }

    private SqlAgreementsRetirementStatements getStatements() {
        return statements == null ? new PostgresAgreementRetirementStatements() : statements;
    }
}
