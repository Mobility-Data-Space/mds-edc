package eu.dataspace.dataplane.dfrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dataspace.connector.agreements.retirement.spi.event.ContractAgreementRetired;
import eu.dataspace.dataplane.dfrs.subscriber.ReNegotiateObserver;
import eu.dataspace.dataplane.dfrs.subscriber.SendEventToObserver;
import eu.dataspace.dataplane.dfrs.subscriber.StartObserverTransfer;
import eu.dataspace.dataplane.dfrs.subscriber.StoreObserverAddress;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Clock;
import java.util.function.Supplier;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.spi.query.Criterion.criterion;


public class DfrsObserverManager {

    public static final String DFRS_OBSERVER_ADDRESS_KEY = "dfrs-observer-address";

    private final Monitor monitor;
    private final ContractNegotiationService negotiationService;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final ParticipantContextSupplier participantContextSupplier;
    private final CatalogService catalogService;
    private final JsonLd jsonLd;
    private final Supplier<ObjectMapper> mapperSupplier;
    private final EventRouter eventRouter;
    private final TransferProcessService transferProcessService;
    private final Vault vault;
    private final EdcHttpClient httpClient;
    private final Clock clock;

    public DfrsObserverManager(Monitor monitor, ContractNegotiationService negotiationService,
                               TypeTransformerRegistry typeTransformerRegistry, ParticipantContextSupplier participantContextSupplier,
                               CatalogService catalogService, JsonLd jsonLd, Supplier<ObjectMapper> mapperSupplier,
                               EventRouter eventRouter, TransferProcessService transferProcessService, Vault vault,
                               EdcHttpClient httpClient, Clock clock) {
        this.monitor = monitor;
        this.negotiationService = negotiationService;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.participantContextSupplier = participantContextSupplier;
        this.catalogService = catalogService;
        this.jsonLd = jsonLd;
        this.mapperSupplier = mapperSupplier;
        this.eventRouter = eventRouter;
        this.transferProcessService = transferProcessService;
        this.vault = vault;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    public Result<Void> activate(DfrsObserverConfig configuration) {
        var participantContextServiceResult = participantContextSupplier.get();
        if (participantContextServiceResult.failed()) {
            return Result.failure("Cannot obtain ParticipantContextSupplier: " + participantContextServiceResult.getFailureDetail());
        }

        var participantContext = participantContextServiceResult.getContent();

        eventRouter.register(ContractNegotiationFinalized.class,
                new StartObserverTransfer(configuration, participantContext, transferProcessService, monitor));

        eventRouter.register(TransferProcessStarted.class,
                new StoreObserverAddress(configuration, participantContext, mapperSupplier, vault, monitor));

        var sendEventToObserver = new SendEventToObserver(participantContext, mapperSupplier, monitor, httpClient, vault, clock);
        eventRouter.register(ContractNegotiationFinalized.class, sendEventToObserver);
        eventRouter.register(TransferProcessStarted.class, sendEventToObserver);
        eventRouter.register(ContractAgreementRetired.class, sendEventToObserver);

        eventRouter.register(TransferProcessTerminated.class, new ReNegotiateObserver(configuration, participantContext, this, transferProcessService));

        var query = QuerySpec.Builder.newInstance()
                .filter(criterion("counterPartyId", "=", configuration.id()))
                .filter(criterion("counterPartyAddress", "=", configuration.url()))
                .filter(criterion("state", "=", FINALIZED.code()))
                .filter(criterion("contractAgreement.assetId", "=", configuration.datasetId()))
                .build();
        var negotiations = negotiationService.search(query)
                .orElseThrow(f -> new EdcException("Cannot setup DFRS observer: " + f));

        if (!negotiations.isEmpty()) {
            monitor.info("DFRS Observer agreement already set up");
            return Result.success();
        }

        negotiateObserverOffer(configuration, participantContext);

        return Result.success();
    }

    public void negotiateObserverOffer(DfrsObserverConfig configuration, ParticipantContext participantContext) {
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

    private @NotNull Result<ContractRequest> toContractRequest(DfrsObserverConfig configuration, ParticipantContext participantContext, Dataset dataset) {
        var offer = dataset.getOffers().entrySet().stream().findFirst().orElse(null);
        if (offer == null) {
            return Result.failure("DFRS Datsset contains no offers");
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
