package eu.dataspace.dataplane.dfrs.model;

import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;

public record ObserverTransferProcessStarted(
        String transferProcessId,
        String assetId,
        String type,
        String contractId,
        String protocol
) implements ObserverEvent {

    public static ObserverTransferProcessStarted from(TransferProcessStarted event) {
        return new ObserverTransferProcessStarted(
                event.getTransferProcessId(), event.getAssetId(), event.getType(), event.getContractId(), event.getProtocol()
        );
    }

    @Override
    public String eventType() {
        return "org.eclipse.edc.TransferProcessStarted";
    }
}
