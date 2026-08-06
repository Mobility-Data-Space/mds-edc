package eu.dataspace.dataplane.dfrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DfrsObserverManagerTest {

    private final Monitor monitor = mock();
    private final ContractNegotiationService negotiationService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final ParticipantContextSupplier participantContextSupplier = mock();
    private final CatalogService catalogService = mock();
    private final JsonLd jsonLd = mock();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();

    private DfrsObserverManager manager;
    private DfrsObserverConfig config;

    @BeforeEach
    void setUp() {
        manager = new DfrsObserverManager(monitor, negotiationService, typeTransformerRegistry,
                participantContextSupplier, catalogService, jsonLd, () -> objectMapper);
        config = new DfrsObserverConfig("provider-id", "http://provider-url", "dataset-id", "dataspace-protocol-http:2025-1");
    }

    @Nested
    class Act {

        @Test
        void shouldReturnSuccessWithoutStartingNegotiation_whenFinalizedNegotiationAlreadyExists() {
            var existingNegotiation = ContractNegotiation.Builder.newInstance()
                    .id("neg-1")
                    .counterPartyId("provider-id")
                    .counterPartyAddress("http://provider-url")
                    .protocol("dataspace-protocol-http:2025-1")
                    .build();
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(existingNegotiation)));

            var result = manager.negotiateObserverContract(config);

            assertThat(result.succeeded()).isTrue();
            verify(monitor).info(any(String.class));
            verifyNoInteractions(participantContextSupplier, catalogService);
        }

        @Test
        void shouldThrow_whenNegotiationSearchFails() {
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.badRequest("search error"));

            assertThatThrownBy(() -> manager.negotiateObserverContract(config))
                    .isInstanceOf(EdcException.class)
                    .hasMessageContaining("Cannot setup DFRS observer");
        }

        @Test
        void shouldReturnFailure_whenParticipantContextSupplierFails() {
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of()));
            when(participantContextSupplier.get()).thenReturn(ServiceResult.badRequest("context error"));

            var result = manager.negotiateObserverContract(config);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureDetail()).contains("Cannot obtain ParticipantContextSupplier");
            verifyNoInteractions(catalogService);
        }

        @Test
        void shouldReturnSuccessImmediately_whenNegotiationIsTriggered() {
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of()));
            when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext()));
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(new CompletableFuture<>());

            var result = manager.negotiateObserverContract(config);

            assertThat(result.succeeded()).isTrue();
        }

        @Test
        void shouldFilterSearchByCounterPartyIdAddressStateAndAsset() {
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of()));
            when(participantContextSupplier.get()).thenReturn(ServiceResult.badRequest("stop early"));

            manager.negotiateObserverContract(config);

            var queryCaptor = ArgumentCaptor.forClass(QuerySpec.class);
            verify(negotiationService).search(queryCaptor.capture());
            var filters = queryCaptor.getValue().getFilterExpression();
            assertThat(filters).anySatisfy(c -> {
                assertThat(c.getOperandLeft()).isEqualTo("counterPartyId");
                assertThat(c.getOperandRight()).isEqualTo("provider-id");
            });
            assertThat(filters).anySatisfy(c -> {
                assertThat(c.getOperandLeft()).isEqualTo("counterPartyAddress");
                assertThat(c.getOperandRight()).isEqualTo("http://provider-url");
            });
            assertThat(filters).anySatisfy(c -> {
                assertThat(c.getOperandLeft()).isEqualTo("contractAgreement.assetId");
                assertThat(c.getOperandRight()).isEqualTo("dataset-id");
            });
        }

        @Test
        void shouldRequestDatasetWithCorrectArguments() {
            var participantContext = participantContext();
            when(negotiationService.search(any())).thenReturn(ServiceResult.success(List.of()));
            when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext));
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(new CompletableFuture<>());

            manager.negotiateObserverContract(config);

            verify(catalogService).requestDataset(
                    eq(participantContext),
                    eq("dataset-id"),
                    eq("provider-id"),
                    eq("http://provider-url"),
                    eq("dataspace-protocol-http:2025-1")
            );
        }
    }

    @Nested
    class NegotiateContract {

        private final ParticipantContext participantContext = participantContext();

        @BeforeEach
        void setUp() {
            when(negotiationService.search(any())).thenReturn(ServiceResult.success(List.of()));
            when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext));
        }

        @Test
        void shouldLogSevere_whenCatalogRequestCompletesExceptionally() {
            var future = new CompletableFuture<StatusResult<byte[]>>();
            when(catalogService.requestDataset(any(), any(), any(), any(), any())).thenReturn(future);

            manager.negotiateObserverContract(config);
            future.completeExceptionally(new RuntimeException("network error"));

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenCatalogResultIsFailure() {
            var failedStatus = StatusResult.<byte[]>failure(ResponseStatus.FATAL_ERROR, "not found");
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(failedStatus));

            manager.negotiateObserverContract(config);

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenJsonDeserializationFails() {
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success("not json".getBytes())));

            manager.negotiateObserverContract(config);

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenJsonLdExpansionFails() throws Exception {
            var rawBytes = objectMapper.writeValueAsBytes(Json.createObjectBuilder().build());
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.failure("expansion failed"));

            manager.negotiateObserverContract(config);

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenTransformerFails() throws Exception {
            var parsedJson = Json.createObjectBuilder().build();
            var rawBytes = objectMapper.writeValueAsBytes(parsedJson);
            var expandedJson = Json.createObjectBuilder().build();
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.success(expandedJson));
            when(typeTransformerRegistry.transform(eq(expandedJson), eq(Dataset.class))).thenReturn(Result.failure("transform failed"));

            manager.negotiateObserverContract(config);

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenDatasetHasNoOffers() throws Exception {
            var parsedJson = Json.createObjectBuilder().build();
            var rawBytes = objectMapper.writeValueAsBytes(parsedJson);
            var expandedJson = Json.createObjectBuilder().build();
            var datasetWithoutOffers = Dataset.Builder.newInstance().id("dataset-id").build();
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.success(expandedJson));
            when(typeTransformerRegistry.transform(eq(expandedJson), eq(Dataset.class))).thenReturn(Result.success(datasetWithoutOffers));

            manager.negotiateObserverContract(config);

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenInitiateNegotiationFails() throws Exception {
            var rawBytes = objectMapper.writeValueAsBytes(Json.createObjectBuilder().build());
            var expandedJson = Json.createObjectBuilder().build();
            var policy = Policy.Builder.newInstance().build();
            var dataset = Dataset.Builder.newInstance().id("dataset-id").offer("offer-id", policy).build();
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.success(expandedJson));
            when(typeTransformerRegistry.transform(eq(expandedJson), eq(Dataset.class))).thenReturn(Result.success(dataset));
            when(negotiationService.initiateNegotiation(eq(participantContext), any()))
                    .thenReturn(ServiceResult.badRequest("negotiation failed"));

            manager.negotiateObserverContract(config);

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldInitiateNegotiationWithCorrectRequest_whenFullChainSucceeds() throws Exception {
            var rawBytes = objectMapper.writeValueAsBytes(Json.createObjectBuilder().build());
            var expandedJson = Json.createObjectBuilder().build();
            var policy = Policy.Builder.newInstance().build();
            var dataset = Dataset.Builder.newInstance().id("dataset-id").offer("offer-id", policy).build();
            var negotiation = ContractNegotiation.Builder.newInstance()
                    .id("neg-1").counterPartyId("provider-id").counterPartyAddress("http://provider-url").protocol("p").build();

            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.success(expandedJson));
            when(typeTransformerRegistry.transform(eq(expandedJson), eq(Dataset.class))).thenReturn(Result.success(dataset));
            when(negotiationService.initiateNegotiation(eq(participantContext), any())).thenReturn(ServiceResult.success(negotiation));

            manager.negotiateObserverContract(config);

            var requestCaptor = ArgumentCaptor.forClass(ContractRequest.class);
            verify(negotiationService).initiateNegotiation(eq(participantContext), requestCaptor.capture());
            var request = requestCaptor.getValue();
            assertThat(request.getContractOffer().getAssetId()).isEqualTo("dataset-id");
            assertThat(request.getContractOffer().getId()).isEqualTo("offer-id");
            assertThat(request.getCounterPartyAddress()).isEqualTo("http://provider-url");
            assertThat(request.getProfile()).isEqualTo("dataspace-protocol-http:2025-1");
        }

        @Test
        void shouldSetPolicyAssignerAndAssigneeFromConfigAndContext_whenBuildingContractRequest() throws Exception {
            var rawBytes = objectMapper.writeValueAsBytes(Json.createObjectBuilder().build());
            var expandedJson = Json.createObjectBuilder().build();
            var policy = Policy.Builder.newInstance().build();
            var dataset = Dataset.Builder.newInstance().id("dataset-id").offer("offer-id", policy).build();
            var negotiation = ContractNegotiation.Builder.newInstance()
                    .id("neg-1").counterPartyId("provider-id").counterPartyAddress("http://provider-url").protocol("p").build();

            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.success(expandedJson));
            when(typeTransformerRegistry.transform(eq(expandedJson), eq(Dataset.class))).thenReturn(Result.success(dataset));
            when(negotiationService.initiateNegotiation(eq(participantContext), any())).thenReturn(ServiceResult.success(negotiation));

            manager.negotiateObserverContract(config);

            var requestCaptor = ArgumentCaptor.forClass(ContractRequest.class);
            verify(negotiationService).initiateNegotiation(eq(participantContext), requestCaptor.capture());
            var contractPolicy = requestCaptor.getValue().getContractOffer().getPolicy();
            assertThat(contractPolicy.getAssigner()).isEqualTo("provider-id");
            assertThat(contractPolicy.getAssignee()).isEqualTo("consumer-identity");
            assertThat(contractPolicy.getTarget()).isEqualTo("dataset-id");
        }
    }

    private ParticipantContext participantContext() {
        return ParticipantContext.Builder.newInstance()
                .participantContextId("consumer-participant-id")
                .identity("consumer-identity")
                .build();
    }
}
