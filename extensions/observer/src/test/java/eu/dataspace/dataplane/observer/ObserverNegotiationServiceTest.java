package eu.dataspace.dataplane.observer;

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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObserverNegotiationServiceTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final ContractNegotiationService negotiationService = mock();
    private final CatalogService catalogService = mock();
    private final Monitor monitor = mock();
    private final JsonLd jsonLd = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final ObserverNegotiationService service = new ObserverNegotiationService(catalogService, typeTransformerRegistry,
            negotiationService, monitor, () -> objectMapper, jsonLd);

    @Nested
    class NegotiationAlreadySetup {
        @Test
        void shouldReturnTrue_whenNegotiationExists() {
            var existingNegotiation = ContractNegotiation.Builder.newInstance()
                    .id("neg-1")
                    .counterPartyId("provider-id")
                    .counterPartyAddress("http://provider-url")
                    .protocol("dataspace-protocol-http:2025-1")
                    .build();
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(existingNegotiation)));

            var result = service.isNegotiationAlreadySetup(createConfig());

            assertThat(result).isTrue();
        }

        @Test
        void shouldThrow_whenNegotiationSearchFails() {
            when(negotiationService.search(any(QuerySpec.class))).thenReturn(ServiceResult.badRequest("search error"));

            assertThatThrownBy(() -> service.isNegotiationAlreadySetup(createConfig()))
                    .isInstanceOf(EdcException.class)
                    .hasMessageContaining("Cannot setup DFRS observer");
        }

    }

    @Nested
    class Negotiate {

        @Test
        void shouldLogSevere_whenCatalogRequestCompletesExceptionally() {
            var future = new CompletableFuture<StatusResult<byte[]>>();
            when(catalogService.requestDataset(any(), any(), any(), any(), any())).thenReturn(future);

            service.negotiateObserverOffer(createConfig(), createParticipantContext());
            future.completeExceptionally(new RuntimeException("network error"));

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenCatalogResultIsFailure() {
            var failedStatus = StatusResult.<byte[]>failure(ResponseStatus.FATAL_ERROR, "not found");
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(failedStatus));

            service.negotiateObserverOffer(createConfig(), createParticipantContext());

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenJsonDeserializationFails() {
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success("not json".getBytes())));

            service.negotiateObserverOffer(createConfig(), createParticipantContext());

            verify(monitor).severe(any(String.class), any(Throwable.class));
        }

        @Test
        void shouldLogSevere_whenJsonLdExpansionFails() throws Exception {
            var rawBytes = objectMapper.writeValueAsBytes(Json.createObjectBuilder().build());
            when(catalogService.requestDataset(any(), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(StatusResult.success(rawBytes)));
            when(jsonLd.expand(any(JsonObject.class))).thenReturn(Result.failure("expansion failed"));

            service.negotiateObserverOffer(createConfig(), createParticipantContext());

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

            service.negotiateObserverOffer(createConfig(), createParticipantContext());

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

            service.negotiateObserverOffer(createConfig(), createParticipantContext());

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
            when(negotiationService.initiateNegotiation(any(), any()))
                    .thenReturn(ServiceResult.badRequest("negotiation failed"));

            service.negotiateObserverOffer(createConfig(), createParticipantContext());

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
            when(negotiationService.initiateNegotiation(any(), any())).thenReturn(ServiceResult.success(negotiation));
            var config = createConfig();

            service.negotiateObserverOffer(config, createParticipantContext());

            var requestCaptor = ArgumentCaptor.forClass(ContractRequest.class);
            verify(negotiationService).initiateNegotiation(any(), requestCaptor.capture());
            var request = requestCaptor.getValue();
            assertThat(request.getContractOffer().getAssetId()).isEqualTo(config.datasetId());
            assertThat(request.getContractOffer().getId()).isEqualTo("offer-id");
            assertThat(request.getCounterPartyAddress()).isEqualTo(config.url());
            assertThat(request.getProfile()).isEqualTo(config.profile());
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
            when(negotiationService.initiateNegotiation(any(), any())).thenReturn(ServiceResult.success(negotiation));
            var config = createConfig();
            var participantContext = createParticipantContext();

            service.negotiateObserverOffer(config, participantContext);

            var requestCaptor = ArgumentCaptor.forClass(ContractRequest.class);
            verify(negotiationService).initiateNegotiation(any(), requestCaptor.capture());
            var contractPolicy = requestCaptor.getValue().getContractOffer().getPolicy();
            assertThat(contractPolicy.getAssigner()).isEqualTo(config.id());
            assertThat(contractPolicy.getAssignee()).isEqualTo(participantContext.getIdentity());
            assertThat(contractPolicy.getTarget()).isEqualTo(config.datasetId());
        }
    }

    private static ParticipantContext createParticipantContext() {
        return ParticipantContext.Builder.newInstance().participantContextId("any").identity("any").build();
    }

    private static @NonNull ObserverConfig createConfig() {
        return new ObserverConfig("any", "any", "any", "any", "any", Duration.ofSeconds(1));
    }

}