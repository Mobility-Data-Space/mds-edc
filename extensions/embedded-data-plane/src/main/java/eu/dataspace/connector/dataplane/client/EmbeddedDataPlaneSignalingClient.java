package eu.dataspace.connector.dataplane.client;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResumeMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartedNotificationMessage;
import org.eclipse.edc.signaling.domain.DataFlowStatusMessage;
import org.eclipse.edc.signaling.domain.DataFlowSuspendMessage;
import org.eclipse.edc.signaling.domain.DataFlowTerminateMessage;
import org.eclipse.edc.signaling.port.DataPlaneSignalingClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EmbeddedDataPlaneSignalingClient extends DataPlaneSignalingClient {

    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;
    private final AssetIndex assetIndex;

    public EmbeddedDataPlaneSignalingClient(DataPlaneInstance dataPlane, DataPlaneManager dataPlaneManager,
                                            Monitor monitor, AssetIndex assetIndex) {
        super(dataPlane, null, null, null);
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
        this.assetIndex = assetIndex;
    }

    @Override
    public StatusResult<DataFlowStatusMessage> prepare(DataFlowPrepareMessage request) {
        var messageTransformation = SignalingMapper.toFrameworkProvisionMessage(request);
        if (messageTransformation.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Cannot transform incoming prepare message: " + messageTransformation.getFailureDetail());
        }

        var message = messageTransformation.getContent();

        return dataPlaneManager.provision(message).map(response -> statusMessage(message.getProcessId(), response));
    }

    @Override
    public StatusResult<DataFlowStatusMessage> start(DataFlowStartMessage request) {
        var messageTransformation = SignalingMapper.toFrameworkStartMessage(request, assetIndex);
        if (messageTransformation.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Cannot transform incoming start message: " + messageTransformation.getFailureDetail());
        }

        var message = messageTransformation.getContent();

        return startFlow(message);
    }

    @Override
    public StatusResult<Void> suspend(String flowId, DataFlowSuspendMessage message) {
        return dataPlaneManager.suspend(flowId);
    }

    @Override
    public StatusResult<DataFlowStatusMessage> resume(String flowId, DataFlowResumeMessage message) {
        // the legacy framework has no native resume: map it onto start() for the existing flow
        var startMessage = SignalingMapper.toFrameworkResumeMessage(flowId, message);
        return startFlow(startMessage);
    }

    @Override
    public StatusResult<Void> terminate(String flowId, DataFlowTerminateMessage message) {
        return dataPlaneManager.terminate(flowId);
    }

    @Override
    public StatusResult<Void> started(String flowId, DataFlowStartedNotificationMessage message) {
        // consumer-side notification: not acted upon by the legacy framework, acknowledged as a no-op stub
        monitor.debug("Received 'started' notification for data flow %s".formatted(flowId));
        return StatusResult.success();
    }

    @Override
    public StatusResult<Void> completed(String flowId) {
        // consumer-side notification: not acted upon by the legacy framework, acknowledged as a no-op stub
        monitor.debug("Received 'completed' notification for data flow %s".formatted(flowId));
        return StatusResult.success();
    }

    private @NotNull StatusResult<DataFlowStatusMessage> startFlow(org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage message) {
        return dataPlaneManager.start(message)
                .map(response -> statusMessage(message.getProcessId(), response));
    }

    private DataFlowStatusMessage statusMessage(String flowId, DataFlowResponseMessage response) {
        var dataAddress = Optional.ofNullable(response).map(DataFlowResponseMessage::getDataAddress).orElse(null);
        return SignalingMapper.statusMessage(flowId, dataPlaneManager.getTransferState(flowId), dataAddress);
    }
}
