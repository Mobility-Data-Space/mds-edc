package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.PostgresqlExtension;
import eu.dataspace.connector.tests.SovityDapsExtension;
import eu.dataspace.connector.tests.VaultExtension;
import jakarta.json.Json;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class ContractNegotiationManualApprovalTest {

    @RegisterExtension
    @Order(0)
    private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

    @RegisterExtension
    @Order(1)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer");

    @RegisterExtension
    @Order(2)
    private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

    @RegisterExtension
    @Order(3)
    private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

    @RegisterExtension
    @Order(3)
    private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

    @Test
    void shouldManuallyApproveNegotiation() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);
        var consumerContractNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
        });

        var pending = await().until(PROVIDER::getPendingNegotiations, array -> array.size() == 1).getFirst();
        assertThat(pending.asJsonObject().getString("state")).isEqualTo(REQUESTED.name());

        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/approve", pending.asJsonObject().getString(ID))
                .then()
                .statusCode(204);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(FINALIZED.name());
        });
    }

    @Test
    void shouldManuallyRejectNegotiation() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);

        var consumerContractNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(REQUESTED.name());
        });

        var pending = await().until(PROVIDER::getPendingNegotiations, array -> array.size() == 1).getFirst();
        assertThat(pending.asJsonObject().getString("state")).isEqualTo(REQUESTED.name());

        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/reject", pending.asJsonObject().getString(ID))
                .then()
                .statusCode(204);

        await().untilAsserted(() -> {
            assertThat(CONSUMER.getContractNegotiationState(consumerContractNegotiationId)).isEqualTo(TERMINATED.name());
        });
    }

    @Test
    void shouldManuallyApproveNegotiationWithEvents() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);
        var consumerNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        var contractNegotiationRequested = PROVIDER.waitForEvent("ContractNegotiationRequested");

        var providerNegotiationId = contractNegotiationRequested.getJsonObject("payload").getString("contractNegotiationId");

        CONSUMER.waitForEvent("ContractNegotiationRequested");
        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/approve", providerNegotiationId)
                .then()
                .statusCode(204);

        PROVIDER.waitForEvent("ContractNegotiationManuallyApproved");
        var contractNegotiationFinalized = CONSUMER.waitForEvent("ContractNegotiationFinalized");

        assertThat(contractNegotiationFinalized.getJsonObject("payload").getString("contractNegotiationId")).isEqualTo(consumerNegotiationId);
        assertThat(CONSUMER.getContractNegotiationState(consumerNegotiationId)).isEqualTo(FINALIZED.name());
    }

    @Test
    void shouldManuallyRejectNegotiationWithEvents() {
        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost/any"
        );

        var assetId = createOfferWithManualApproval(dataAddressProperties);
        var consumerNegotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        var contractNegotiationRequested = PROVIDER.waitForEvent("ContractNegotiationRequested");

        var providerNegotiationId = contractNegotiationRequested.getJsonObject("payload").getString("contractNegotiationId");

        CONSUMER.waitForEvent("ContractNegotiationRequested");
        PROVIDER.baseManagementRequest()
                .post("/v3/contractnegotiations/{id}/reject", providerNegotiationId)
                .then()
                .statusCode(204);

        PROVIDER.waitForEvent("ContractNegotiationManuallyRejected");
        var contractNegotiationFinalized = CONSUMER.waitForEvent("ContractNegotiationTerminated");

        assertThat(contractNegotiationFinalized.getJsonObject("payload").getString("contractNegotiationId")).isEqualTo(consumerNegotiationId);
        assertThat(CONSUMER.getContractNegotiationState(consumerNegotiationId)).isEqualTo(TERMINATED.name());
    }

    private String createOfferWithManualApproval(Map<String, Object> dataAddressProperties) {
        var assetId = UUID.randomUUID().toString();
        PROVIDER.createAsset(assetId, Map.of("http://purl.org/dc/terms/title", "any"), dataAddressProperties);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());

        var requestBody = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                .add(EDC_NAMESPACE + "accessPolicyId", noConstraintPolicyId)
                .add(EDC_NAMESPACE + "contractPolicyId", noConstraintPolicyId)
                .add(EDC_NAMESPACE + "assetsSelector", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "Criterion")
                                .add(EDC_NAMESPACE + "operandLeft", EDC_NAMESPACE + "id")
                                .add(EDC_NAMESPACE + "operator", "=")
                                .add(EDC_NAMESPACE + "operandRight", assetId)
                                .build())
                        .build())
                .add(EDC_NAMESPACE + "privateProperties", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "manualApproval", "true"))
                .build();

        PROVIDER.baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v3/contractdefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);

        return assetId;
    }

}
