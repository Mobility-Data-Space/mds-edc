package eu.dataspace.dataplane.dfrs;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StartObserverTransferTest {

    private final TransferProcessService transferProcessService = mock();
    private final Monitor monitor = mock();
    private final String observerParticipantId = "provider-id";
    private final DfrsObserverConfig config = new DfrsObserverConfig(
            observerParticipantId, "http://provider-url", "dataset-id",
            "dataspace-protocol-http:2025-1", "HttpData-PULL");
    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("consumer-participant-id")
            .identity("consumer-identity")
            .build();

    private final StartObserverTransfer subscriber = new StartObserverTransfer(
            config, participantContext, transferProcessService, monitor);

    @Test
    void shouldDoNothing_whenEventIsNotContractNegotiationFinalized() {
        var event = mock(Event.class);

        subscriber.on(envelope(event));

        verifyNoInteractions(transferProcessService, monitor);
    }

    @Test
    void shouldDoNothing_whenCounterPartyNotObserver() {
        when(transferProcessService.initiateTransfer(any(), any())).thenReturn(ServiceResult.success(mock()));
        var agreement = contractAgreement("agreement-1");
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyId("another-participant")
                .counterPartyAddress("http://provider-url")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(agreement)
                .build();

        subscriber.on(envelope(event));

        verifyNoInteractions(transferProcessService);
    }

    @Test
    void shouldInitiateTransfer_whenContractNegotiationFinalizedReceivedOnObserverProvider() {
        when(transferProcessService.initiateTransfer(any(), any())).thenReturn(ServiceResult.success(mock()));
        var agreement = contractAgreement("agreement-1");
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyId(observerParticipantId)
                .counterPartyAddress("http://provider-url")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(agreement)
                .build();

        subscriber.on(envelope(event));

        var captor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferProcessService).initiateTransfer(eq(participantContext), captor.capture());
        var request = captor.getValue();
        assertThat(request.getContractId()).isEqualTo("agreement-1");
        assertThat(request.getCounterPartyAddress()).isEqualTo("http://provider-url");
        assertThat(request.getProfile()).isEqualTo("dataspace-protocol-http:2025-1");
        assertThat(request.getTransferType()).isEqualTo("HttpData-PULL");
    }

    @Test
    void shouldLogSevere_whenInitiateTransferFails() {
        when(transferProcessService.initiateTransfer(any(), any()))
                .thenReturn(ServiceResult.badRequest("transfer error"));
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyId(observerParticipantId)
                .counterPartyAddress("http://provider-url")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(contractAgreement("agreement-1"))
                .build();

        subscriber.on(envelope(event));

        verify(monitor).severe(any(String.class));
    }

    private ContractAgreement contractAgreement(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("provider-id")
                .consumerId("consumer-id")
                .assetId("dataset-id")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private <T extends Event> EventEnvelope<ContractNegotiationFinalized> envelope(T event) {
        return EventEnvelope.Builder.newInstance()
                .at(1)
                .payload(event)
                .build();
    }
}
