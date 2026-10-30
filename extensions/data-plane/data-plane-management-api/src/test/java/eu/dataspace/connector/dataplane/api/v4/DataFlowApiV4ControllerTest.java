package eu.dataspace.connector.dataplane.api.v4;

import eu.dataspace.connector.dataplane.api.service.DataFlowService;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.controlplane.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowApiV4ControllerTest extends RestControllerTestBase {

    private final DataFlowService service = mock();
    private final TypeTransformerRegistry transformer = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final Monitor monitor = mock();

    @Test
    void requestDataFlows_shouldReturnFlows() {
        var flow1 = DataFlow.Builder.newInstance().id("flow-1").build();
        var flow2 = DataFlow.Builder.newInstance().id("flow-2").build();

        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(transformer.transform(isA(DataFlow.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(service.findAll(any())).thenReturn(ServiceResult.success(List.of(flow1, flow2)));

        baseRequest()
                .contentType(ContentType.JSON)
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body(notNullValue())
                .body("size()", is(2));
    }

    @Test
    void getDataAddress_shouldReturnAddress() {
        var address = DataAddress.Builder.newInstance().type("HttpData").property("keyName", "source-key-alias").build();
        var addressJson = Json.createObjectBuilder()
                .add(TYPE, "DataAddress")
                .add("type", "HttpData")
                .add("baseUrl", "http://source")
                .build();
        when(service.getDataAddress("flow-1")).thenReturn(ServiceResult.success(address));
        when(transformer.transform(isA(DataAddress.class), eq(JsonObject.class)))
                .thenReturn(Result.success(addressJson));

        baseRequest()
                .get("/{id}/dataaddress", "flow-1")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("'@type'", is("DataAddress"))
                .body("type", is("HttpData"))
                .body("baseUrl", is("http://source"));
    }

    @Test
    void getDataAddress_shouldReturn404_whenNotFound() {
        when(service.getDataAddress("missing")).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .get("/{id}/dataaddress", "missing")
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new DataFlowApiV4Controller(service, transformer, validator, monitor);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v4/dataflows")
                .when();
    }
}
