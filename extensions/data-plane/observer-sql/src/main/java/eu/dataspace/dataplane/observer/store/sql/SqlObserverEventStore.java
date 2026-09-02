package eu.dataspace.dataplane.observer.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.store.PendingObserverEvent;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class SqlObserverEventStore extends AbstractSqlStore implements ObserverEventStore {

    private final ObserverEventStatements statements;

    public SqlObserverEventStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                 TransactionContext transactionContext, ObjectMapper objectMapper,
                                 QueryExecutor queryExecutor, ObserverEventStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public void save(PendingObserverEvent event) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                queryExecutor.execute(connection, statements.upsertTemplate(),
                        event.id(), event.envelopeJson(), event.retryCount(), event.nextRetryAt().toEpochMilli());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public PendingObserverEvent findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                try (var stream = queryExecutor.query(connection, false, this::mapRow, statements.findByIdTemplate(), id)) {
                    return stream.findFirst().orElse(null);
                }
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public List<PendingObserverEvent> nextPending() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var now = Instant.now().toEpochMilli();
                try (var stream = queryExecutor.query(connection, false, this::mapRow, statements.nextPendingTemplate(), now)) {
                    return stream.toList();
                }
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void delete(String id) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                queryExecutor.execute(connection, statements.deleteByIdTemplate(), id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private PendingObserverEvent mapRow(ResultSet rs) throws SQLException {
        return new PendingObserverEvent(
                rs.getString(statements.getIdColumn()),
                rs.getString(statements.getEnvelopeJsonColumn()),
                rs.getInt(statements.getRetryCountColumn()),
                Instant.ofEpochMilli(rs.getLong(statements.getNextRetryAtColumn()))
        );
    }
}
