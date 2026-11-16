package eu.dataspace.connector.dataplane.api.service;

import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.controlplane.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowServiceImplTest {

    private final DataPlaneStore store = mock();
    private final TransferProcessStore transferProcessStore = mock();
    private final DataAddressStore dataAddressStore = mock();
    private final DataFlowServiceImpl service = new DataFlowServiceImpl(store, transferProcessStore, dataAddressStore, new NoopTransactionContext());

    @Test
    void findAll_shouldReturnFlows() {
        var flow = DataFlow.Builder.newInstance().id("flow-1").build();
        when(store.findAll(any())).thenReturn(Stream.of(flow));

        var result = service.findAll(QuerySpec.max());

        assertThat(result).isSucceeded().isEqualTo(List.of(flow));
    }

    @Test
    void getDataAddress_shouldResolveFromDataAddressStore() {
        var transferProcess = TransferProcess.Builder.newInstance().id("flow-1").build();
        var dataAddress = DataAddress.Builder.newInstance().type("HttpData").build();
        when(transferProcessStore.findById("flow-1")).thenReturn(transferProcess);
        when(dataAddressStore.resolve(transferProcess)).thenReturn(StoreResult.success(dataAddress));

        var result = service.getDataAddress("flow-1");

        assertThat(result).isSucceeded().isSameAs(dataAddress);
    }

    @Test
    void getDataAddress_shouldReturnNotFound_whenTransferProcessDoesNotExist() {
        when(transferProcessStore.findById("missing")).thenReturn(null);

        var result = service.getDataAddress("missing");

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
    }

    @Test
    void getDataAddress_shouldReturnNotFound_whenDataAddressNotStored() {
        var transferProcess = TransferProcess.Builder.newInstance().id("flow-1").build();
        when(transferProcessStore.findById("flow-1")).thenReturn(transferProcess);
        when(dataAddressStore.resolve(transferProcess)).thenReturn(StoreResult.notFound("no address"));

        var result = service.getDataAddress("flow-1");

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
    }
}
