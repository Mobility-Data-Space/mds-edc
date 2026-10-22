package eu.dataspace.connector.datasource.store.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and SQL table+column names required for the {@link SqlDataSourceStore}.
 */
public interface SqlDataSourceStatements extends SqlStatements {

    default String getIdColumn() {
        return "id";
    }

    default String getNameColumn() {
        return "name";
    }

    default String getTypeColumn() {
        return "type";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    default String getUpdatedAtColumn() {
        return "updated_at";
    }

    default String getTable() {
        return "edc_data_source";
    }

    String insertTemplate();

    String updateTemplate();

    String findByIdTemplate();

    String getDeleteByIdTemplate();

    String getCountByIdClause();

    String getCountVariableName();

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
