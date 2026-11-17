package eu.dataspace.connector.agreements.retirement.api.v3;

import eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static eu.dataspace.connector.agreements.retirement.api.model.AgreementsRetirementEntry.AR_ENTRY_TYPE;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementsRetirementApiV3ControllerTest extends RestControllerTestBase {

    private final ContractAgreementService service = mock();
    private final TypeTransformerRegistry transformer = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final Monitor monitor = mock();

    @Test
    void should_getAllWithoutQuerySpec() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(transformer.transform(isA(AgreementsRetirementEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createJsonRetirementEntry("agreement-1")))
                .thenReturn(Result.success(createJsonRetirementEntry("agreement-2")));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(retiredAgreement("agreement-1"), retiredAgreement("agreement-2"))));

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body(notNullValue())
                .body("size()", is(2));

        var captor = ArgumentCaptor.forClass(QuerySpec.class);
        verify(service).search(captor.capture());
        assertThat(captor.getValue().getFilterExpression()).containsExactly(criterion("retired", "=", true));
    }

    @Test
    void should_translateLegacyQueryFields() {
        var legacyQuery = QuerySpec.Builder.newInstance()
                .filter(List.of(criterion("agreementId", "=", "agreement-1"), criterion(AR_ENTRY_REASON, "like", "%reason%")))
                .sortField("agreementRetirementDate")
                .sortOrder(SortOrder.DESC)
                .offset(5)
                .limit(10)
                .build();
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(legacyQuery));
        when(transformer.transform(isA(AgreementsRetirementEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createJsonRetirementEntry("agreement-1")));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(retiredAgreement("agreement-1"))));

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", is(1));

        var captor = ArgumentCaptor.forClass(QuerySpec.class);
        verify(service).search(captor.capture());
        var query = captor.getValue();
        assertThat(query.getFilterExpression()).containsExactly(
                criterion("retired", "=", true),
                criterion("id", "=", "agreement-1"),
                criterion("retirementReason", "like", "%reason%"));
        assertThat(query.getSortField()).isEqualTo("retirementDate");
        assertThat(query.getSortOrder()).isEqualTo(SortOrder.DESC);
        assertThat(query.getOffset()).isEqualTo(5);
        assertThat(query.getLimit()).isEqualTo(10);
    }

    @Test
    void should_mapRetiredAgreementToLegacyEntry() {
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(retiredAgreement("agreement-1"))));
        when(transformer.transform(isA(AgreementsRetirementEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createJsonRetirementEntry("agreement-1")));

        baseRequest()
                .contentType(ContentType.JSON)
                .post("/request")
                .then()
                .statusCode(200);

        verify(transformer).transform(eq(new AgreementsRetirementEntry("agreement-1", "long-reason", 1234L)), eq(JsonObject.class));
    }

    @Test
    void should_reactivateAgreement() {
        var agreementId = "test-agreement-id";
        when(service.unretireAgreement(agreementId)).thenReturn(ServiceResult.success());

        baseRequest()
                .delete("/{id}", agreementId)
                .then()
                .statusCode(204);
    }

    @Test
    void shouldNot_reactivateAgreement_whenNotExists() {
        var agreementId = "test-agreement-id";
        when(service.unretireAgreement(agreementId)).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .delete("/{id}", agreementId)
                .then()
                .log().ifError()
                .statusCode(404);
    }

    @Test
    void shouldNot_reactivateAgreement_whenNotRetired() {
        var agreementId = "test-agreement-id";
        when(service.unretireAgreement(agreementId)).thenReturn(ServiceResult.conflict("not retired"));

        baseRequest()
                .delete("/{id}", agreementId)
                .then()
                .statusCode(404);
    }

    @Test
    void should_retireAgreement() {
        var agreementId = "test-agreement-id";
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(AgreementsRetirementEntry.class)))
                .thenReturn(Result.success(new AgreementsRetirementEntry(agreementId, "long-reason", 0L)));
        when(service.retireAgreement(agreementId, "long-reason")).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .body(createJsonRetirementEntry(agreementId))
                .post()
                .then()
                .log().ifError()
                .statusCode(204);

        verify(service).retireAgreement(agreementId, "long-reason");
    }

    @Test
    void shouldNot_retireAgreement_whenAlreadyRetired() {
        var agreementId = "test-agreement-id";
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(AgreementsRetirementEntry.class)))
                .thenReturn(Result.success(new AgreementsRetirementEntry(agreementId, "long-reason", 0L)));
        when(service.retireAgreement(any(), any())).thenReturn(ServiceResult.conflict("already retired"));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(createJsonRetirementEntry(agreementId))
                .post()
                .then()
                .statusCode(409);
    }

    @Test
    void shouldNot_retireAgreement_whenNotExists() {
        var agreementId = "test-agreement-id";
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(AgreementsRetirementEntry.class)))
                .thenReturn(Result.success(new AgreementsRetirementEntry(agreementId, "long-reason", 0L)));
        when(service.retireAgreement(any(), any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .contentType(ContentType.JSON)
                .body(createJsonRetirementEntry(agreementId))
                .post()
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new AgreementsRetirementApiV3Controller(service, transformer, validator, monitor);
    }

    private ContractAgreement retiredAgreement(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("provider")
                .consumerId("consumer")
                .assetId("asset")
                .policy(Policy.Builder.newInstance().build())
                .participantContextId("participantContextId")
                .agreementId(id)
                .retired(true)
                .retirementReason("long-reason")
                .retirementDate(1234L)
                .build();
    }

    private JsonObject createJsonRetirementEntry(String agreementId) {
        return Json.createObjectBuilder()
                .add(TYPE, AR_ENTRY_TYPE)
                .add(AR_ENTRY_AGREEMENT_ID, agreementId)
                .add(AR_ENTRY_REASON, "long-reason")
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v3/contractagreements/retirements")
                .when();
    }

}
