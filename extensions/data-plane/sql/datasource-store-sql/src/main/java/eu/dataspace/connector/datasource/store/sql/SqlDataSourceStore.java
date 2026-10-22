package eu.dataspace.connector.datasource.store.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * PostgreSQL {@link DataSourceStore}. The queryable index ({@code id}, {@code name}, {@code type} plus timestamps) is
 * persisted in {@code edc_data_source}, while the opaque {@code properties} are stored in the {@link Vault} keyed by
 * data source id and hydrated on read, mirroring {@code VaultDataAddressStorePatch} and the in-memory store.
 */
public class SqlDataSourceStore extends AbstractSqlStore implements DataSourceStore {

    private static final TypeReference<Map<String, Object>> PROPERTIES_TYPE = new TypeReference<>() {
    };

    private final SqlDataSourceStatements statements;
    private final Vault vault;
    private final Clock clock;

    public SqlDataSourceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                              TransactionContext transactionContext, ObjectMapper objectMapper,
                              QueryExecutor queryExecutor, SqlDataSourceStatements statements, Vault vault, Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.vault = vault;
        this.clock = clock;
    }

    @Override
    public StoreResult<DataSource> create(DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(dataSource.getId(), connection)) {
                    return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(dataSource.getId()));
                }

                var now = clock.millis();
                dataSource.setCreatedAt(now);
                dataSource.setUpdatedAt(now);

                queryExecutor.execute(connection, statements.insertTemplate(),
                        dataSource.getId(), dataSource.getName(), dataSource.getType(),
                        dataSource.getCreatedAt(), dataSource.getUpdatedAt());
                vault.storeSecret(DataSourceStore.vaultKey(dataSource.getId()), toJson(dataSource.getProperties()));

                return StoreResult.success(dataSource);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (!existsById(dataSource.getId(), connection)) {
                    return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(dataSource.getId()));
                }

                queryExecutor.execute(connection, statements.updateTemplate(),
                        dataSource.getName(), dataSource.getType(), clock.millis(), dataSource.getId());
                vault.storeSecret(DataSourceStore.vaultKey(dataSource.getId()), toJson(dataSource.getProperties()));

                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<DataSource> findById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var dataSource = queryExecutor.single(connection, false, this::mapDataSource, statements.findByIdTemplate(), id);
                if (dataSource == null) {
                    return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(id));
                }
                return StoreResult.success(dataSource);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Stream<DataSource> query(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var statement = statements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapDataSource, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (!existsById(id, connection)) {
                    return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(id));
                }
                queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), id);
                vault.deleteSecret(DataSourceStore.vaultKey(id));
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private boolean existsById(String id, Connection connection) {
        try (var stream = queryExecutor.query(connection, false, this::mapRowCount, statements.getCountByIdClause(), id)) {
            return stream.findFirst().orElse(0) > 0;
        }
    }

    private int mapRowCount(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(statements.getCountVariableName());
    }

    private DataSource mapDataSource(ResultSet resultSet) throws SQLException {
        var id = resultSet.getString(statements.getIdColumn());
        return DataSource.Builder.newInstance()
                .id(id)
                .name(resultSet.getString(statements.getNameColumn()))
                .type(resultSet.getString(statements.getTypeColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .properties(fromJson(vault.resolveSecret(DataSourceStore.vaultKey(id)), PROPERTIES_TYPE))
                .build();
    }
}
