package eu.dataspace.connector.datasource.spi.service;

import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Offers the CRUD and query functionality for the {@link DataSource} feature.
 */
public interface DataSourceService {

    /**
     * Creates a new data source.
     *
     * @param dataSource the data source to create.
     * @return the created data source (with id and timestamps set) on success, a conflict failure if it already exists.
     */
    ServiceResult<DataSource> create(DataSource dataSource);

    /**
     * Updates an existing data source.
     *
     * @param dataSource the data source to update.
     * @return success, a not-found failure if it does not exist.
     */
    ServiceResult<Void> update(DataSource dataSource);

    /**
     * Returns the data source with the given id.
     *
     * @param id the data source id.
     * @return the data source on success, a not-found failure otherwise.
     */
    ServiceResult<DataSource> findById(String id);

    /**
     * Returns the data sources matching the query (filtering over {@code name} and {@code type} only).
     *
     * @param querySpec a valid {@link QuerySpec}.
     * @return the matching data sources.
     */
    ServiceResult<List<DataSource>> query(QuerySpec querySpec);

    /**
     * Deletes the data source with the given id.
     *
     * @param id the data source id.
     * @return success, a not-found failure if it does not exist.
     */
    ServiceResult<Void> deleteById(String id);
}
