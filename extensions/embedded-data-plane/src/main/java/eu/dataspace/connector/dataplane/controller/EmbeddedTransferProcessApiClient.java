package eu.dataspace.connector.dataplane.controller;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyPreparedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Permits in-memory communication from data-plane to control-plane instead of passing through the HTTP API.
 * Note: we pass a TransferProcessService supplier to avoid cyclic dependencies between extensions. At the moment there's
 * no way around that. That will be fixed since the Data Plane code will be inherited in this repository.
 */
public class EmbeddedTransferProcessApiClient implements TransferProcessApiClient {
    private final Supplier<TransferProcessService> transferProcessServiceSupplier;

    public EmbeddedTransferProcessApiClient(Supplier<TransferProcessService> transferProcessServiceSupplier) {
        this.transferProcessServiceSupplier = transferProcessServiceSupplier;
    }

    @Override
    public StatusResult<Void> completed(DataFlow dataFlow) {
        return transferProcessServiceSupplier.get().complete(dataFlow.getId())
                .flatMap(this::toStatusResult);
    }

    @Override
    public StatusResult<Void> failed(DataFlow dataFlow, String reason) {
        return transferProcessServiceSupplier.get().terminate(new TerminateTransferCommand(dataFlow.getId(), reason))
                .flatMap(this::toStatusResult);
    }

    @Override
    public StatusResult<Void> provisioned(DataFlow dataFlow) {
        return transferProcessServiceSupplier.get().notifyPrepared(new NotifyPreparedCommand(dataFlow.getId(), dataFlow.provisionedDataAddress()))
                .flatMap(this::toStatusResult);
    }

    private @NotNull StatusResult<Void> toStatusResult(ServiceResult<Void> it) {
        if (it.succeeded()) {
            return StatusResult.success();
        } else {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, it.getFailureDetail());
        }
    }
}
