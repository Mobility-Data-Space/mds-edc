package eu.dataspace.dataplane.observer.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.agreements.retirement.spi.event.ContractAgreementRetired;
import eu.dataspace.dataplane.observer.model.event.ObserverEventStored;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.store.PendingObserverEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StoreObserverEventTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final ObserverEventStore store = mock();
    private final EventRouter eventRouter = mock();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("provider-participant-id")
            .identity("provider-identity")
            .build();

    private final StoreObserverEvent subscriber = new StoreObserverEvent(
            participantContext, () -> objectMapper, store, eventRouter, clock, Duration.ofSeconds(1));

    @Test
    void shouldSaveToStoreAndPublishEvent_onContractNegotiationFinalized() {
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyAddress("http://consumer.example/dsp")
                .counterPartyId("consumer-participant")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(contractAgreement("provider-identity"))
                .build();

        subscriber.on(envelope(event));

        var captor = ArgumentCaptor.forClass(PendingObserverEvent.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().envelopeJson()).isNotBlank();
        assertThat(captor.getValue().retryCount()).isEqualTo(0);
        assertThat(captor.getValue().nextRetryAt()).isEqualTo(clock.instant().plus(Duration.ofSeconds(1)));

        var routerCaptor = ArgumentCaptor.forClass(ObserverEventStored.class);
        verify(eventRouter).publish(routerCaptor.capture());
        assertThat(routerCaptor.getValue().getPendingEventId()).isEqualTo(captor.getValue().id());
    }

    @Test
    void shouldSaveToStoreAndPublishEvent_onTransferProcessStarted() {
        var event = TransferProcessStarted.Builder.newInstance()
                .transferProcessId("tp-1")
                .assetId("asset-1")
                .type(TransferProcess.Type.PROVIDER.name())
                .contractId("contract-1")
                .protocol("dataspace-protocol-http:2025-1")
                .build();

        subscriber.on(envelope(event));

        verify(store).save(any());
        verify(eventRouter).publish(any(ObserverEventStored.class));
    }

    @Test
    void shouldSaveToStoreAndPublishEvent_onContractAgreementRetired() {
        var event = ContractAgreementRetired.Builder.newInstance()
                .contractAgreementId("agreement-1")
                .build();

        subscriber.on(envelope(event));

        verify(store).save(any());
        verify(eventRouter).publish(any(ObserverEventStored.class));
    }

    @Test
    void shouldIgnore_whenContractNegotiationIsConsumerSide() {
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyAddress("http://consumer.example/dsp")
                .counterPartyId("consumer-participant")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(contractAgreement("another-provider"))
                .build();

        subscriber.on(envelope(event));

        verifyNoInteractions(store, eventRouter);
    }

    @Test
    void shouldIgnore_whenTransferProcessIsConsumerSide() {
        var event = TransferProcessStarted.Builder.newInstance()
                .transferProcessId("tp-1")
                .assetId("asset-1")
                .type(TransferProcess.Type.CONSUMER.name())
                .contractId("contract-1")
                .protocol("dataspace-protocol-http:2025-1")
                .build();

        subscriber.on(envelope(event));

        verifyNoInteractions(store, eventRouter);
    }

    @Test
    void shouldDoNothing_whenEventTypeIsNotHandled() {
        subscriber.on(envelope(mock(Event.class)));

        verifyNoInteractions(store, eventRouter);
    }

    @Test
    void shouldPropagateException_whenStoreFails() {
        doThrow(new RuntimeException("store failure")).when(store).save(any());
        var event = ContractAgreementRetired.Builder.newInstance()
                .contractAgreementId("agreement-1")
                .build();

        assertThatThrownBy(() -> subscriber.on(envelope(event)))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(eventRouter);
    }

    private ContractAgreement contractAgreement(String providerId) {
        return ContractAgreement.Builder.newInstance()
                .id("agr-1")
                .providerId(providerId)
                .consumerId("consumer-participant")
                .assetId("asset-1")
                .contractSigningDate(1721469600L)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> EventEnvelope<T> envelope(T event) {
        return EventEnvelope.Builder.newInstance()
                .at(1)
                .payload(event)
                .build();
    }
}
