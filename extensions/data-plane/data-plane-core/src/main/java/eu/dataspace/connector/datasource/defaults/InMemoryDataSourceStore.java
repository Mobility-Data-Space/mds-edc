package eu.dataspace.connector.datasource.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * In-memory implementation of {@link DataSourceStore}. The queryable index ({@code id}, {@code name}, {@code type} plus
 * timestamps) is kept in memory, while the opaque {@code properties} are stored in the {@link Vault} keyed by data
 * source id and hydrated on read, mirroring the way {@code VaultDataAddressStorePatch} keeps addresses out of the
 * connector storage.
 */
public class InMemoryDataSourceStore implements DataSourceStore {

    private final Map<String, DataSource> index = new ConcurrentHashMap<>();
    private final QueryResolver<DataSource> queryResolver;
    private final Vault vault;
    private final ObjectMapper mapper;
    private final Clock clock;

    public InMemoryDataSourceStore(CriterionOperatorRegistry criterionOperatorRegistry, Vault vault, ObjectMapper mapper, Clock clock) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(DataSource.class, criterionOperatorRegistry);
        this.vault = vault;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public StoreResult<DataSource> create(DataSource dataSource) {
        if (index.containsKey(dataSource.getId())) {
            return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(dataSource.getId()));
        }

        var now = clock.millis();
        dataSource.setCreatedAt(now);
        dataSource.setUpdatedAt(now);

        index.put(dataSource.getId(), copyIndex(dataSource));
        vault.storeSecret(dataSource.getId(), serialize(dataSource.getProperties()));

        return StoreResult.success(dataSource);
    }

    @Override
    public StoreResult<Void> update(DataSource dataSource) {
        var existing = index.get(dataSource.getId());
        if (existing == null) {
            return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(dataSource.getId()));
        }

        dataSource.setCreatedAt(existing.getCreatedAt());
        dataSource.setUpdatedAt(clock.millis());

        index.put(dataSource.getId(), copyIndex(dataSource));
        vault.storeSecret(dataSource.getId(), serialize(dataSource.getProperties()));

        return StoreResult.success();
    }

    @Override
    public StoreResult<DataSource> findById(String id) {
        var dataSource = index.get(id);
        if (dataSource == null) {
            return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(id));
        }
        return StoreResult.success(hydrate(dataSource));
    }

    @Override
    public Stream<DataSource> query(QuerySpec querySpec) {
        return queryResolver.query(index.values().stream(), querySpec).map(this::hydrate);
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        if (index.remove(id) == null) {
            return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(id));
        }
        vault.deleteSecret(id);
        return StoreResult.success();
    }

    private DataSource copyIndex(DataSource dataSource) {
        return DataSource.Builder.newInstance()
                .id(dataSource.getId())
                .name(dataSource.getName())
                .type(dataSource.getType())
                .createdAt(dataSource.getCreatedAt())
                .updatedAt(dataSource.getUpdatedAt())
                .build();
    }

    private DataSource hydrate(DataSource indexed) {
        return DataSource.Builder.newInstance()
                .id(indexed.getId())
                .name(indexed.getName())
                .type(indexed.getType())
                .createdAt(indexed.getCreatedAt())
                .updatedAt(indexed.getUpdatedAt())
                .properties(deserialize(vault.resolveSecret(indexed.getId())))
                .build();
    }

    private String serialize(Map<String, Object> properties) {
        try {
            return mapper.writeValueAsString(properties);
        } catch (Exception e) {
            throw new EdcException("Failed to serialize data source properties", e);
        }
    }

    private Map<String, Object> deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(serialized, defaultInstance().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new EdcException("Failed to deserialize data source properties", e);
        }
    }
}
