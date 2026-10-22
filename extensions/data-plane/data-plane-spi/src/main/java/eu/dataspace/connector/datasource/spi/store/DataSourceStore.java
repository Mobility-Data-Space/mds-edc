package eu.dataspace.connector.datasource.spi.store;

import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.stream.Stream;

/**
 * Persistence point for {@link DataSource} entities. Only {@code id}, {@code name} and {@code type} form the queryable
 * index; the {@code properties} are opaque and stored in the vault, keyed by data source id, and hydrated on read.
 */
@ExtensionPoint
public interface DataSourceStore {

    String ALREADY_EXISTS_TEMPLATE = "Data Source with id %s already exists";
    String NOT_FOUND_TEMPLATE = "Data Source with id %s not found";

    String VAULT_KEY_PREFIX = "data-source-";

    /**
     * Builds the vault key under which a data source's {@code properties} are stored, prefixing the id to avoid
     * collisions with other secrets kept in the same vault.
     *
     * @param id the data source id.
     * @return the vault key.
     */
    static String vaultKey(String id) {
        return VAULT_KEY_PREFIX + id;
    }

    /**
     * Persists a new data source.
     *
     * @param dataSource the data source to create.
     * @return the stored data source on success, an already-exists failure if the id is already present.
     */
    StoreResult<DataSource> create(DataSource dataSource);

    /**
     * Updates an existing data source.
     *
     * @param dataSource the data source to update.
     * @return success, a not-found failure if the id does not exist.
     */
    StoreResult<Void> update(DataSource dataSource);

    /**
     * Returns the data source with the given id, properties hydrated from the vault.
     *
     * @param id the data source id.
     * @return the data source on success, a not-found failure otherwise.
     */
    StoreResult<DataSource> findById(String id);

    /**
     * Returns the data sources matching the query (filtering over {@code name} and {@code type} only).
     *
     * @param querySpec a valid {@link QuerySpec}.
     * @return a stream of matching data sources, properties hydrated from the vault.
     */
    Stream<DataSource> query(QuerySpec querySpec);

    /**
     * Deletes the data source with the given id, removing its vault-stored properties.
     *
     * @param id the data source id.
     * @return success, a not-found failure if the id does not exist.
     */
    StoreResult<Void> deleteById(String id);

}
