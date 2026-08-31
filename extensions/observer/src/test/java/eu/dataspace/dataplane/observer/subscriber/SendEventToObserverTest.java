package eu.dataspace.dataplane.observer.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.resource.ResourceLoader;
import eu.dataspace.connector.agreements.retirement.spi.event.ContractAgreementRetired;
import okhttp3.Request;
import okio.Buffer;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SendEventToObserverTest {

    private static final String SCHEMA_BASE_IRI = "https://mds.example/schemas/";

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final EdcHttpClient httpClient = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("provider-participant-id")
            .identity("provider-identity")
            .build();

    private final SendEventToObserver subscriber = new SendEventToObserver(
            participantContext, () -> objectMapper, monitor, httpClient, vault, clock);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property("endpoint", "http://observer.example/api/v1/events")
                .property("authorization", "Bearer test-token")
                .build();
        when(vault.resolveSecret(any(), any())).thenReturn(objectMapper.writeValueAsString(dataAddress));
        doAnswer(inv -> Result.success(null)).when(httpClient).execute(any(Request.class), any(Function.class));
    }

    @Test
    void shouldSendSchemaCompliantContractNegotiationFinalizedEvent() throws IOException {
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyAddress("http://consumer.example/dsp")
                .counterPartyId("consumer-participant")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(contractAgreement("provider-identity"))
                .build();

        subscriber.on(envelope(event));

        assertThat(loadSchema().validate(captureRequestBodyAsJson())).isEmpty();
    }

    @Test
    void shouldIgnoreContractNegotiationFinalizedEvent_whenConsumerNegotiation() {
        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("neg-1")
                .counterPartyAddress("http://consumer.example/dsp")
                .counterPartyId("consumer-participant")
                .protocol("dataspace-protocol-http:2025-1")
                .contractAgreement(contractAgreement("another-provider"))
                .build();

        subscriber.on(envelope(event));

        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldSendSchemaCompliantTransferProcessStartedEvent() throws IOException {
        var event = TransferProcessStarted.Builder.newInstance()
                .transferProcessId("tp-1")
                .assetId("asset-1")
                .type(TransferProcess.Type.PROVIDER.name())
                .contractId("contract-1")
                .protocol("dataspace-protocol-http:2025-1")
                .build();

        subscriber.on(envelope(event));

        assertThat(loadSchema().validate(captureRequestBodyAsJson())).isEmpty();
    }

    @Test
    void shouldIgnoreTransferProcessStartedEvent_whenConsumerTransfer() {
        var event = TransferProcessStarted.Builder.newInstance()
                .transferProcessId("tp-1")
                .assetId("asset-1")
                .type(TransferProcess.Type.CONSUMER.name())
                .contractId("contract-1")
                .protocol("dataspace-protocol-http:2025-1")
                .build();

        subscriber.on(envelope(event));

        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldSendSchemaCompliantContractAgreementRetiredEvent() throws IOException {
        var event = ContractAgreementRetired.Builder.newInstance()
                .contractAgreementId("agreement-1")
                .build();

        subscriber.on(envelope(event));

        assertThat(loadSchema().validate(captureRequestBodyAsJson())).isEmpty();
    }

    @Test
    void shouldLogSevere_whenVaultDoesNotContainDataAddress() {
        when(vault.resolveSecret(any(), any())).thenReturn(null);

        subscriber.on(envelope(ContractAgreementRetired.Builder.newInstance()
                .contractAgreementId("agreement-1")
                .build()));

        verify(monitor).severe(any(String.class));
        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldDoNothing_whenEventTypeIsNotHandled() {
        subscriber.on(envelope(mock(Event.class)));

        verifyNoInteractions(httpClient, monitor);
    }

    private Schema loadSchema() {
        var schemaBaseIri = "https://mds.example/schemas/";
        var schemaClasspathPrefix = "/schemas/v1/";

        ResourceLoader resourceLoader = iri -> {
            var path = schemaClasspathPrefix + iri.toString().replace(schemaBaseIri, "");
            return () -> getClass().getResourceAsStream(path);
        };

        var registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12, registryBuilder ->
                registryBuilder.schemaLoader(loaderBuilder ->
                        loaderBuilder.resourceLoaders(rlBuilder -> rlBuilder.add(resourceLoader))));

        return registry.getSchema(getClass().getResourceAsStream(schemaClasspathPrefix + "event-envelope.json"));
    }

    @SuppressWarnings("unchecked")
    private JsonNode captureRequestBodyAsJson() throws IOException {
        var captor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient).execute(captor.capture(), any(Function.class));
        var buffer = new Buffer();
        captor.getValue().body().writeTo(buffer);
        return objectMapper.readTree(buffer.readUtf8());
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
