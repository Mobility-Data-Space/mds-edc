package eu.dataspace.dataplane.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.dataplane.observer.subscriber.StartObserverTransfer;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObserverManagerTest {

    private final Monitor monitor = mock();
    private final ParticipantContextSupplier participantContextSupplier = mock();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final EventRouter eventRouter = mock();
    private final ObserverNegotiationService observerNegotiationService = mock();

    private final ObserverManager manager = new ObserverManager(monitor,
            participantContextSupplier, () -> objectMapper, eventRouter, mock(), mock(), mock(), mock(), mock(), mock(),
            observerNegotiationService);
    private final ObserverConfig config = new ObserverConfig("provider-id", "http://provider-url",
            "dataset-id", "dataspace-protocol-http:2025-1", "HttpData-PULL", Duration.ofSeconds(30));

    @Nested
    class Activate {

        @Test
        void shouldReturnFailure_whenParticipantContextSupplierFails() {
            when(participantContextSupplier.get()).thenReturn(ServiceResult.badRequest("context error"));

            var result = manager.activate(config);

            assertThat(result).isFailed().messages()
                    .anyMatch(it -> it.contains("Cannot obtain ParticipantContextSupplier"));
            verifyNoInteractions(observerNegotiationService);
        }

        @Test
        void shouldReturnSuccessWithoutStartingNegotiation_whenFinalizedNegotiationAlreadyExists() {
            when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext()));
            when(observerNegotiationService.isNegotiationAlreadySetup(any())).thenReturn(true);

            var result = manager.activate(config);

            assertThat(result.succeeded()).isTrue();
            verify(monitor).info(any(String.class));
            verify(eventRouter).register(eq(ContractNegotiationFinalized.class), isA(StartObserverTransfer.class));
            verify(observerNegotiationService, never()).negotiateObserverOffer(any(), any());
        }

        @Test
        void shouldReturnSuccessImmediately_whenNegotiationIsTriggered() {
            when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext()));
            when(observerNegotiationService.isNegotiationAlreadySetup(any())).thenReturn(false);
            doNothing().when(observerNegotiationService).negotiateObserverOffer(any(), any());

            var result = manager.activate(config);

            assertThat(result.succeeded()).isTrue();
            verify(observerNegotiationService).negotiateObserverOffer(any(), any());
        }
    }

    private ParticipantContext participantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId("consumer-participant-id")
                .identity("consumer-identity")
                .build();
    }
}
