package eu.dataspace.dataplane.observer.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.dataplane.observer.ObserverConfig;
import eu.dataspace.dataplane.observer.ObserverManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;

import java.util.function.Supplier;

import static eu.dataspace.dataplane.observer.ObserverManager.OBSERVER_ADDRESS_KEY;

/**
 * Listens for {@link org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted} events
 * and, when the started transfer process is identified as the observer transfer (matching the configured dataset ID
 * and carrying a non-null data address), persists the observer's data address in the vault under the key
 * {@link ObserverManager#OBSERVER_ADDRESS_KEY}. This stored address is
 * later used by {@link SendEventToObserver} to reach the observer endpoint.
 */
public class StoreObserverAddress implements EventSubscriber {
    private final ObserverConfig configuration;
    private final ParticipantContext participantContext;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final Vault vault;
    private final Monitor monitor;

    public StoreObserverAddress(ObserverConfig configuration, ParticipantContext participantContext,
                                Supplier<ObjectMapper> mapperSupplier, Vault vault, Monitor monitor) {
        this.configuration = configuration;
        this.participantContext = participantContext;
        this.mapperSupplier = mapperSupplier;
        this.vault = vault;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        // this event subscriber could eventually be replaced with a listener on the DPS "Started" event (https://github.com/Mobility-Data-Space/mds-edc/issues/558)
        if (event.getPayload() instanceof TransferProcessStarted transferProcessStarted && isObserverTransfer(transferProcessStarted)) {
            try {
                var json = mapperSupplier.get().writeValueAsString(transferProcessStarted.getDataAddress());
                vault.storeSecret(participantContext.getParticipantContextId(), OBSERVER_ADDRESS_KEY, json)
                        .onSuccess(i -> {
                            monitor.info("DFRS Observer address stored in vault with key " + OBSERVER_ADDRESS_KEY);
                        })
                        .onFailure(f -> {
                            monitor.severe("Cannot store DFRS Observer address: " + f.getFailureDetail());

                        });
            } catch (JsonProcessingException e) {
                monitor.severe("Cannot serialize to JSON", e);
            }
        }
    }

    private boolean isObserverTransfer(TransferProcessStarted transferProcessStarted) {
        return transferProcessStarted.getAssetId().equals(configuration.datasetId()) && transferProcessStarted.getDataAddress() != null;
    }
}
