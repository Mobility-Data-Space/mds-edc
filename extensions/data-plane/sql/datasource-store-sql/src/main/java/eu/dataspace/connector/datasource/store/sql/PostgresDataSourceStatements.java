package eu.dataspace.connector.datasource.store.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class PostgresDataSourceStatements implements SqlDataSourceStatements {

    private final SqlOperatorTranslator operatorTranslator;

    public PostgresDataSourceStatements() {
        this.operatorTranslator = new PostgresqlOperatorTranslator();
    }

    @Override
    public String insertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getNameColumn())
                .column(getTypeColumn())
                .column(getCreatedAtColumn())
                .column(getUpdatedAtColumn())
                .insertInto(getTable());
    }

    @Override
    public String updateTemplate() {
        return executeStatement()
                .column(getNameColumn())
                .column(getTypeColumn())
                .column(getUpdatedAtColumn())
                .update(getTable(), getIdColumn());
    }

    @Override
    public String findByIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getTable(), getIdColumn());
    }

    @Override
    public String getCountByIdClause() {
        return format("SELECT COUNT (*) FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String getCountVariableName() {
        return "COUNT";
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getTable());
        return new SqlQueryStatement(select, querySpec, new DataSourceMapping(this), operatorTranslator);
    }
}
