package eu.dataspace.dataplane.observer.store.sql;

import org.eclipse.edc.sql.statement.SqlExecuteStatement;
import org.eclipse.edc.sql.statement.SqlStatements;

import static java.lang.String.format;

public class ObserverEventStatements implements SqlStatements {

    @Override
    public SqlExecuteStatement executeStatement() {
        return SqlExecuteStatement.newInstance("::jsonb");
    }

    public String getIdColumn() {
        return "id";
    }

    public String getEnvelopeJsonColumn() {
        return "envelope_json";
    }

    public String getRetryCountColumn() {
        return "retry_count";
    }

    public String getNextRetryAtColumn() {
        return "next_retry_at";
    }

    public String getTable() {
        return "mds_observer_event";
    }

    public String upsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .jsonColumn(getEnvelopeJsonColumn())
                .column(getRetryCountColumn())
                .column(getNextRetryAtColumn())
                .upsertInto(getTable(), getIdColumn());
    }

    public String findByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    public String nextPendingTemplate() {
        return format("SELECT * FROM %s WHERE %s <= ?", getTable(), getNextRetryAtColumn());
    }

    public String deleteByIdTemplate() {
        return executeStatement().delete(getTable(), getIdColumn());
    }
}
