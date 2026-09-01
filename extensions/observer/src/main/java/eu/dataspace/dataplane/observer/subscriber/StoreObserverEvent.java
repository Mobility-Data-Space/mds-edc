package eu.dataspace.dataplane.observer.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.agreements.retirement.spi.event.ContractAgreementRetired;
import eu.dataspace.dataplane.observer.model.ObserverContractAgreementRetired;
import eu.dataspace.dataplane.observer.model.ObserverContractNegotiationFinalized;
import eu.dataspace.dataplane.observer.model.ObserverEventEnvelope;
import eu.dataspace.dataplane.observer.model.ObserverTransferProcessStarted;
import eu.dataspace.dataplane.observer.model.event.ObserverEventStored;
import eu.dataspace.dataplane.observer.store.ObserverEventStore;
import eu.dataspace.dataplane.observer.store.PendingObserverEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class StoreObserverEvent implements EventSubscriber {

    private final ParticipantContext participantContext;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final ObserverEventStore store;
    private final EventRouter eventRouter;
    private final Clock clock;
    private final Duration retryInterval;

    public StoreObserverEvent(ParticipantContext participantContext, Supplier<ObjectMapper> mapperSupplier,
                              ObserverEventStore store, EventRouter eventRouter, Clock clock, Duration retryInterval) {
        this.participantContext = participantContext;
        this.mapperSupplier = mapperSupplier;
        this.store = store;
        this.eventRouter = eventRouter;
        this.clock = clock;
        this.retryInterval = retryInterval;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var observerEvent = switch (event.getPayload()) {
            case ContractNegotiationFinalized finalized when isProviderNegotiation(finalized) ->
                    ObserverContractNegotiationFinalized.from(finalized);
            case TransferProcessStarted started when isProviderTransfer(started) ->
                    ObserverTransferProcessStarted.from(started);
            case ContractAgreementRetired retired ->
                    ObserverContractAgreementRetired.from(retired);
            default -> null;
        };

        Optional.ofNullable(observerEvent)
                .map(e -> ObserverEventEnvelope.create(e, participantContext, clock))
                .ifPresent(this::storeAndPublish);
    }

    private void storeAndPublish(ObserverEventEnvelope envelope) {
        var pending = new PendingObserverEvent(UUID.randomUUID().toString(), toJson(envelope), 0, clock.instant().plus(retryInterval));
        store.save(pending);
        eventRouter.publish(new ObserverEventStored(pending.id()));
    }

    private String toJson(Object object) {
        try {
            return mapperSupplier.get().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize observer event envelope", e);
        }
    }

    private boolean isProviderTransfer(TransferProcessStarted started) {
        return started.getType().equals(TransferProcess.Type.PROVIDER.name());
    }

    private boolean isProviderNegotiation(ContractNegotiationFinalized finalized) {
        return finalized.getContractAgreement().getProviderId().equals(participantContext.getIdentity());
    }
}
