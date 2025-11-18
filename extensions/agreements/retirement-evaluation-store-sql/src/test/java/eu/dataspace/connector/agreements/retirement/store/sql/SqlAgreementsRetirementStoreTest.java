package eu.dataspace.connector.agreements.retirement.store.sql;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import eu.dataspace.connector.agreements.retirement.store.AgreementsRetirementStoreTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@PostgresqlIntegrationTest
class SqlAgreementsRetirementStoreTest extends AgreementsRetirementStoreTestBase {
    private final TypeManager typeManager = new JacksonTypeManager();
    private final SqlAgreementsRetirementStatements statements = new PostgresAgreementRetirementStatements();
    private SqlAgreementsRetirementStore store;

    @RegisterExtension
    static PostgresqlStoreSetupExtension extension =
            new PostgresqlStoreSetupExtension("postgres:18.1");

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        store = new SqlAgreementsRetirementStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTable() + " CASCADE");
    }

    @Override
    protected AgreementsRetirementStore getStore() {
        return store;
    }
}
