package eu.dataspace.dataplane.observer.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.dataplane.observer.store.PendingObserverEvent;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.function.Supplier;

import static eu.dataspace.dataplane.observer.ObserverManager.OBSERVER_ADDRESS_KEY;

public class ObserverEventDispatcher {

    private static final String APPLICATION_JSON = "application/json";

    private final Supplier<ObjectMapper> mapperSupplier;
    private final EdcHttpClient httpClient;
    private final Vault vault;
    private final ParticipantContext participantContext;

    public ObserverEventDispatcher(Supplier<ObjectMapper> mapperSupplier, EdcHttpClient httpClient,
                                   Vault vault, ParticipantContext participantContext) {
        this.mapperSupplier = mapperSupplier;
        this.httpClient = httpClient;
        this.vault = vault;
        this.participantContext = participantContext;
    }

    public Result<Void> dispatch(PendingObserverEvent pending) {
        var dataAddressResult = readDataAddress();
        if (dataAddressResult.failed()) {
            return Result.failure(dataAddressResult.getFailureDetail());
        }
        var dataAddress = dataAddressResult.getContent();

        var requestBody = RequestBody.create(pending.envelopeJson(), MediaType.get(APPLICATION_JSON));
        var request = new Request.Builder()
                .url(dataAddress.getStringProperty("endpoint"))
                .addHeader("Authorization", dataAddress.getStringProperty("authorization"))
                .post(requestBody)
                .build();

        return httpClient.execute(request, response -> {
            if (!response.isSuccessful()) {
                return Result.failure("HTTP " + response.code());
            }
            return Result.success(null);
        });
    }

    private Result<DataAddress> readDataAddress() {
        try {
            var json = vault.resolveSecret(participantContext.getParticipantContextId(), OBSERVER_ADDRESS_KEY);
            if (json == null) {
                return Result.failure("No data-address stored in the vault");
            }
            var deserialized = mapperSupplier.get().readValue(json, DataAddress.class);
            return Result.success(deserialized);
        } catch (JsonProcessingException e) {
            return Result.failure("Cannot deserialize DataAddress: " + e.getMessage());
        }
    }
}
