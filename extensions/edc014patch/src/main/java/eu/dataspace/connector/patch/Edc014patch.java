package eu.dataspace.connector.patch;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;

import java.sql.SQLException;

/**
 * This patch is needed because the db migrations are drown into tractusx-edc, which still hasn't upgraded to EDC 0.14
 */
public class Edc014patch implements ServiceExtension {

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Override
    public void start() {
        var defaultDataSource = dataSourceRegistry.resolve("default");
        var queryExecutor = new SqlQueryExecutor();
        try (var connection = defaultDataSource.getConnection()) {
            queryExecutor.execute(connection, "ALTER TABLE edc_data_plane ADD COLUMN IF NOT EXISTS resource_definitions json default '[]';");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
