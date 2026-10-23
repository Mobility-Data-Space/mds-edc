package eu.dataspace.connector.datasource.store.sql;

import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.store.DataSourceStoreTestBase;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@PostgresqlIntegrationTest
class SqlDataSourceStoreTest extends DataSourceStoreTestBase {

    private final TypeManager typeManager = new JacksonTypeManager();
    private final SqlDataSourceStatements statements = new PostgresDataSourceStatements();
    private final Vault vault = new InMemoryVault();
    private SqlDataSourceStore store;

    @RegisterExtension
    static PostgresqlStoreSetupExtension extension =
            new PostgresqlStoreSetupExtension("postgres:18.1");

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        store = new SqlDataSourceStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements, vault, Clock.systemUTC());

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTable() + " CASCADE");
    }

    @Override
    protected DataSourceStore getStore() {
        return store;
    }

    private static class InMemoryVault implements Vault {
        private final Map<String, String> secrets = new ConcurrentHashMap<>();

        @Override
        public @Nullable String resolveSecret(String key) {
            return secrets.get(key);
        }

        @Override
        public Result<Void> storeSecret(String key, String value) {
            secrets.put(key, value);
            return Result.success();
        }

        @Override
        public Result<Void> deleteSecret(String key) {
            return secrets.remove(key) == null ? Result.failure("not found") : Result.success();
        }
    }
}
