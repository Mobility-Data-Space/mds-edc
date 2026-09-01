package eu.dataspace.dataplane.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Supplier;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class ObserverNegotiationService {

    private final CatalogService catalogService;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final ContractNegotiationService negotiationService;
    private final Monitor monitor;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final JsonLd jsonLd;

    public ObserverNegotiationService(CatalogService catalogService, TypeTransformerRegistry typeTransformerRegistry,
                                      ContractNegotiationService negotiationService, Monitor monitor, Supplier<ObjectMapper> mapperSupplier,
                                      JsonLd jsonLd) {
        this.catalogService = catalogService;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.negotiationService = negotiationService;
        this.monitor = monitor;
        this.mapperSupplier = mapperSupplier;
        this.jsonLd = jsonLd;
    }

    public boolean isNegotiationAlreadySetup(ObserverConfig configuration) {
        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("counterPartyId", "=", configuration.id()))
                .filter(criterion("counterPartyAddress", "=", configuration.url()))
                .filter(criterion("state", "=", FINALIZED.code()))
                .filter(criterion("contractAgreement.assetId", "=", configuration.datasetId()))
                .build();
        var negotiations = negotiationService.search(query)
                .orElseThrow(f -> new EdcException("Cannot setup DFRS observer: " + f));

        return !negotiations.isEmpty();
    }

    public void negotiateObserverOffer(ObserverConfig configuration, ParticipantContext participantContext) {
        catalogService
                .requestDataset(participantContext, configuration.datasetId(), configuration.id(), configuration.url(), configuration.profile())
                .thenAccept(datasetResult -> {
                    datasetResult
                            .flatMap(this::toResult)
                            .compose(this::fromJson)
                            .compose(jsonLd::expand)
                            .compose(j -> typeTransformerRegistry.transform(j, Dataset.class))
                            .compose(dataset -> toContractRequest(configuration, participantContext, dataset))
                            .compose(request -> negotiationService.initiateNegotiation(participantContext, request)
                                    .flatMap(this::toResult))
                            .orElseThrow(failure -> new EdcException("DFRS Observer Agreement setup failure: " + failure.getFailureDetail()));

                })
                .exceptionally(throwable -> {
                    monitor.severe("DFRS Observer agreement cannot be setup", throwable);
                    return null;
                });
    }

    private @NotNull Result<ContractRequest> toContractRequest(ObserverConfig configuration, ParticipantContext participantContext, Dataset dataset) {
        var offer = dataset.getOffers().entrySet().stream().findFirst().orElse(null);
        if (offer == null) {
            return Result.failure("DFRS Dataset contains no offers");
        }

        var policy = offer.getValue()
                .toBuilder()
                .assigner(configuration.id())
                .assignee(participantContext.getIdentity())
                .target(configuration.datasetId())
                .build();
        var request = ContractRequest.Builder.newInstance()
                .profile(configuration.profile())
                .counterPartyAddress(configuration.url())
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(offer.getKey())
                        .policy(policy)
                        .assetId(configuration.datasetId())
                        .build())
                .build();

        return Result.success(request);
    }

    private Result<JsonObject> fromJson(byte[] datasetBytes) {
        try {
            return Result.success(mapperSupplier.get().readValue(datasetBytes, JsonObject.class));
        } catch (IOException e) {
            return Result.failure("Cannot deserialize Dataset: " + e.getMessage());
        }
    }

    private @NotNull <T, R extends AbstractResult<T, ?, R>> Result<T> toResult(R it) {
        if (it.succeeded()) {
            return Result.success(it.getContent());
        } else {
            return Result.failure(it.getFailureDetail());
        }
    }
}
