package eu.dataspace.connector.datasource.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.store.DataSourceStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataSourceStoreTest extends DataSourceStoreTestBase {

    private final InMemoryDataSourceStore store = new InMemoryDataSourceStore(
            CriterionOperatorRegistryImpl.ofDefaults(), new InMemoryTestVault(), new ObjectMapper(), Clock.systemUTC());

    @Override
    protected DataSourceStore getStore() {
        return store;
    }

    private static class InMemoryTestVault implements Vault {
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
