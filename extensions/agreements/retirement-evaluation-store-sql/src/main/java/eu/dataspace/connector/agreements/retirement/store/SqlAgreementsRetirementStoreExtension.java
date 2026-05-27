package eu.dataspace.connector.agreements.retirement.store;

import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.store.sql.PostgresAgreementRetirementStatements;
import eu.dataspace.connector.agreements.retirement.store.sql.SqlAgreementsRetirementStatements;
import eu.dataspace.connector.agreements.retirement.store.sql.SqlAgreementsRetirementStore;
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

@Extension(value = SqlAgreementsRetirementStoreExtension.NAME)
public class SqlAgreementsRetirementStoreExtension implements ServiceExtension {

    protected static final String NAME = "SQL Agreement Retirement Store.";

    @Setting(key = "edc.sql.store.agreementretirement.datasource",
            description = "Datasource name for the SQL AgreementsRetirement store",
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

    @Inject(required = false)
    private SqlAgreementsRetirementStatements statements;

    @Provider
    public AgreementsRetirementStore sqlStore() {
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
