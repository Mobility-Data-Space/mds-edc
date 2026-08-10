package eu.dataspace.dataplane.dfrs.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.dataplane.dfrs.model.ObserverEventEnvelope;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.time.Clock;
import java.util.function.Supplier;

import static eu.dataspace.dataplane.dfrs.DfrsObserverManager.DFRS_OBSERVER_ADDRESS_KEY;

/**
 * Listens for {@link org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized}
 * events on the provider side and forwards a CloudEvents-compliant {@link eu.dataspace.dataplane.dfrs.model.ObserverEventEnvelope}
 * to the DFRS observer endpoint. The observer's data address (including endpoint URL and authorization token)
 * is read from the vault before dispatching the HTTP request.
 */
public class SendEventToObserver implements EventSubscriber {
    private final ParticipantContext participantContext;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private final Vault vault;
    private final Clock clock;

    public SendEventToObserver(ParticipantContext participantContext, Supplier<ObjectMapper> mapperSupplier,
                               Monitor monitor, EdcHttpClient httpClient, Vault vault, Clock clock) {
        this.participantContext = participantContext;
        this.mapperSupplier = mapperSupplier;
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.vault = vault;
        this.clock = clock;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof ContractNegotiationFinalized finalized && isProviderNegotiation(finalized)) {

            var dataAddressRetrieval = readDataAddress();
            if (dataAddressRetrieval.failed()) {
                monitor.severe("Failed to retrieve data address for DFRS observer. The event won't be dispatched: " + dataAddressRetrieval.getFailureDetail());
                return;
            }
            var dataAddress = dataAddressRetrieval.getContent();

            var eventEnvelope = ObserverEventEnvelope.create(finalized, participantContext, clock);

            serializeToJson(eventEnvelope)
                    .map(body -> RequestBody.create(body, MediaType.get(eventEnvelope.datacontenttype())))
                    .map(requestBody -> new Request.Builder()
                            .url(dataAddress.getStringProperty("endpoint"))
                            .addHeader("Authorization", dataAddress.getStringProperty("authorization"))
                            .post(requestBody)
                            .build())
                    .compose(request -> httpClient.execute(request, response -> {
                        if (response.isSuccessful()) {
                            monitor.debug("Event %s sent to observer".formatted(eventEnvelope.type()));
                        } else {
                            monitor.severe("Error in sending event to observer. Status: %s".formatted(response.code()));
                        }
                        return null;
                    }))
                    .onFailure(failure -> monitor
                            .severe("Exception in sending event to observer: " + failure.getFailureDetail()));
        }
    }

    private boolean isProviderNegotiation(ContractNegotiationFinalized finalized) {
        return finalized.getContractAgreement().getProviderId().equals(participantContext.getIdentity());
    }

    private Result<String> serializeToJson(ObserverEventEnvelope eventEnvelope) {
        try {
            var json = mapperSupplier.get().writeValueAsString(eventEnvelope);
            return Result.success(json);
        } catch (JsonProcessingException e) {
            return Result.failure("Cannot serialize envelope: " + e.getMessage());
        }
    }

    private Result<DataAddress> readDataAddress() {
        try {
            var json = vault.resolveSecret(participantContext.getParticipantContextId(), DFRS_OBSERVER_ADDRESS_KEY);
            if (json == null) {
                return Result.failure("No data-address stored in the vault");
            }
            var deserialized = mapperSupplier.get().readValue(json, DataAddress.class);
            return Result.success(deserialized);
        } catch (JsonProcessingException e) {
            return Result.failure("cannot get DataAddress");
        }
    }
}
