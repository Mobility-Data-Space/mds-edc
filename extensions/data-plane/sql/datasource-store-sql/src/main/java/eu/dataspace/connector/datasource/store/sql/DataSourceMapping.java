package eu.dataspace.connector.datasource.store.sql;

import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps the queryable {@code DataSource} fields to their columns. Only {@code name} and {@code type} are meant to be
 * filtered over through the API; {@code id} and the timestamps are mapped too so internal lookups and ordering work.
 */
public class DataSourceMapping extends TranslationMapping {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    DataSourceMapping(SqlDataSourceStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_NAME, statements.getNameColumn());
        add(FIELD_TYPE, statements.getTypeColumn());
        add(FIELD_CREATED_AT, statements.getCreatedAtColumn());
        add(FIELD_UPDATED_AT, statements.getUpdatedAtColumn());
    }
}
