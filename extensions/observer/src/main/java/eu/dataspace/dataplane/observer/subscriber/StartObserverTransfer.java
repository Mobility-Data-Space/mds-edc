package eu.dataspace.dataplane.observer.subscriber;

import eu.dataspace.dataplane.observer.ObserverConfig;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Listens for {@link org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized}
 * events and, when the finalized negotiation is identified as belonging to the configured DFRS observer
 * (matching both the observer participant ID and dataset ID), initiates a transfer process toward the observer.
 */
public class StartObserverTransfer implements EventSubscriber {
    private final ObserverConfig configuration;
    private final ParticipantContext participantContext;
    private final TransferProcessService transferProcessService;
    private final Monitor monitor;

    public StartObserverTransfer(ObserverConfig configuration, ParticipantContext participantContext,
                                 TransferProcessService transferProcessService, Monitor monitor) {
        this.configuration = configuration;
        this.participantContext = participantContext;
        this.transferProcessService = transferProcessService;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof ContractNegotiationFinalized finalized && isObserverNegotiation(finalized)) {
            var transferRequest = TransferRequest.Builder.newInstance()
                    .profile(configuration.profile())
                    .counterPartyAddress(configuration.url())
                    .contractId(finalized.getContractAgreement().getId())
                    .transferType(configuration.transferProfile())
                    // this data destination added only to trigger consumer data-plane provisioning. it should change once we adopt DPS (https://github.com/Mobility-Data-Space/mds-edc/issues/558)
                    .dataDestination(DataAddress.Builder.newInstance().type("HttpData").build())
                    .build();

            transferProcessService.initiateTransfer(participantContext, transferRequest)
                    .onFailure(failure -> monitor.severe("Cannot Initiate DFRS Observer transfer: " + failure.getFailureDetail()));
        }
    }

    private boolean isObserverNegotiation(ContractNegotiationFinalized finalized) {
        return finalized.getCounterPartyId().equals(configuration.id()) && finalized.getContractAgreement().getAssetId().equals(configuration.datasetId());
    }
}
