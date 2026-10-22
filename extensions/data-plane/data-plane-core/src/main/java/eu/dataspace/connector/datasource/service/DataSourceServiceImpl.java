package eu.dataspace.connector.datasource.service;

import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import eu.dataspace.connector.datasource.spi.store.DataSourceStore;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default {@link DataSourceService}, delegating to the {@link DataSourceStore} within a transaction.
 */
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceStore store;
    private final TransactionContext transactionContext;

    public DataSourceServiceImpl(DataSourceStore store, TransactionContext transactionContext) {
        this.store = store;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<DataSource> create(DataSource dataSource) {
        return transactionContext.execute(() -> {
            if (dataSource.getId() == null) {
                dataSource.setId(UUID.randomUUID().toString());
            }
            return ServiceResult.from(store.create(dataSource));
        });
    }

    @Override
    public ServiceResult<Void> update(DataSource dataSource) {
        return transactionContext.execute(() -> ServiceResult.from(store.update(dataSource)));
    }

    @Override
    public ServiceResult<DataSource> findById(String id) {
        return transactionContext.execute(() -> ServiceResult.from(store.findById(id)));
    }

    @Override
    public ServiceResult<List<DataSource>> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.success(store.query(querySpec).collect(Collectors.toList())));
    }

    @Override
    public ServiceResult<Void> deleteById(String id) {
        return transactionContext.execute(() -> ServiceResult.from(store.deleteById(id)));
    }
}
