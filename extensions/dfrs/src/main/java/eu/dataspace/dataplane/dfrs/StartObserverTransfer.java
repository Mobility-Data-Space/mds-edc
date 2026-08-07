package eu.dataspace.dataplane.dfrs;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

class StartObserverTransfer implements EventSubscriber {
    private final DfrsObserverConfig configuration;
    private final ParticipantContext participantContext;
    private final TransferProcessService transferProcessService;
    private final Monitor monitor;

    public StartObserverTransfer(DfrsObserverConfig configuration, ParticipantContext participantContext,
                                 TransferProcessService transferProcessService, Monitor monitor) {
        this.configuration = configuration;
        this.participantContext = participantContext;
        this.transferProcessService = transferProcessService;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof ContractNegotiationFinalized finalized && finalized.getCounterPartyId().equals(configuration.id())) {
            var transferRequest = TransferRequest.Builder.newInstance()
                    .profile(configuration.profile())
                    .counterPartyAddress(configuration.url())
                    .contractId(finalized.getContractAgreement().getId())
                    .profile(configuration.profile())
                    .transferType(configuration.transferProfile())
                    .build();

            transferProcessService.initiateTransfer(participantContext, transferRequest)
                    .onFailure(failure -> monitor.severe("Cannot Initiate DFRS Observer transfer: " + failure.getFailureDetail()));
        }
    }
}
