package eu.dataspace.dataplane.observer.store.sql;

import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.store.ObserverEventStoreTestBase;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class SqlObserverEventStoreTest extends ObserverEventStoreTestBase {

    private final TypeManager typeManager = new JacksonTypeManager();
    private final ObserverEventStatements statements = new ObserverEventStatements();
    private SqlObserverEventStore store;

    @RegisterExtension
    static PostgresqlStoreSetupExtension extension = new PostgresqlStoreSetupExtension("postgres:18.1");

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        store = new SqlObserverEventStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTable() + " CASCADE");
    }

    @Override
    protected ObserverEventStore getStore() {
        return store;
    }
}
