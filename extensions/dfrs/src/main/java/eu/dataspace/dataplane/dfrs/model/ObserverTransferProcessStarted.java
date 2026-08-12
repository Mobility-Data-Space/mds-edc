package eu.dataspace.dataplane.dfrs.model;

public record ObserverTransferProcessStarted(
        String transferProcessId,
        String assetId,
        String type,
        String contractId,
        String protocol
) implements ObserverEvent {
}
