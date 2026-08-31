package eu.dataspace.dataplane.observer.subscriber;

import eu.dataspace.dataplane.observer.ObserverConfig;
import eu.dataspace.dataplane.observer.ObserverManager;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;

public class ReNegotiateObserver implements EventSubscriber {
    private final ObserverConfig configuration;
    private final ParticipantContext participantContext;
    private final ObserverManager manager;
    private final TransferProcessService transferProcessService;

    public ReNegotiateObserver(ObserverConfig configuration, ParticipantContext participantContext, ObserverManager manager, TransferProcessService transferProcessService) {
        this.configuration = configuration;
        this.participantContext = participantContext;
        this.manager = manager;
        this.transferProcessService = transferProcessService;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof TransferProcessTerminated terminated && isObserverTransfer(terminated)) {
            manager.negotiateObserverOffer(configuration, participantContext);
        }
    }

    private boolean isObserverTransfer(TransferProcessTerminated terminated) {
        var transferProcess = transferProcessService.findById(terminated.getTransferProcessId());
        return transferProcess != null
                && transferProcess.getType() == TransferProcess.Type.CONSUMER
                && transferProcess.getAssetId().equals(configuration.datasetId());
    }
}
