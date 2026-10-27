package eu.dataspace.connector.dataplane.api.service;

import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.controlplane.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link DataFlowService}, delegating to the {@link DataPlaneStore} for querying and to the
 * {@link DataAddressStore} for the data address lookup.
 */
public class DataFlowServiceImpl implements DataFlowService {

    private final DataPlaneStore store;
    private final TransferProcessStore transferProcessStore;
    private final DataAddressStore dataAddressStore;
    private final TransactionContext transactionContext;

    public DataFlowServiceImpl(DataPlaneStore store, TransferProcessStore transferProcessStore,
                               DataAddressStore dataAddressStore, TransactionContext transactionContext) {
        this.store = store;
        this.transferProcessStore = transferProcessStore;
        this.dataAddressStore = dataAddressStore;
        this.transactionContext = transactionContext;
    }

    @Override
    public ServiceResult<List<DataFlow>> findAll(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.success(store.findAll(querySpec).collect(Collectors.toList())));
    }

    @Override
    public ServiceResult<DataAddress> getDataAddress(String dataFlowId) {
        return transactionContext.execute(() -> {
            // a data flow id matches the transfer process id it originated from; the data address is held by the
            // DataAddressStore, keyed by transfer process, and resolved (secret included) from there
            var transferProcess = transferProcessStore.findById(dataFlowId);
            if (transferProcess == null) {
                return ServiceResult.<DataAddress>notFound("No transfer process found for data flow %s".formatted(dataFlowId));
            }
            return ServiceResult.from(dataAddressStore.resolve(transferProcess));
        });
    }
}
