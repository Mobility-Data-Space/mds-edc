package eu.dataspace.connector.datasource.api.v4;

import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import eu.dataspace.connector.datasource.spi.types.DataSource;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSourceApiV4ControllerTest extends RestControllerTestBase {

    private final DataSourceService service = mock();
    private final TypeTransformerRegistry transformer = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final Monitor monitor = mock();

    @Test
    void create_shouldReturn201() {
        var dataSource = dataSource("ds-1");
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(DataSource.class))).thenReturn(Result.success(dataSource));
        when(service.create(any())).thenReturn(ServiceResult.success(dataSource));
        when(transformer.transform(any(), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add(ID, "ds-1").build()));

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .post()
                .then()
                .log().ifError()
                .statusCode(201)
                .body("'@id'", is("ds-1"));
    }

    @Test
    void update_shouldReturn204() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(DataSource.class))).thenReturn(Result.success(dataSource("ds-1")));
        when(service.update(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .put()
                .then()
                .log().ifError()
                .statusCode(204);
    }

    @Test
    void update_shouldReturn404_whenNotFound() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(DataSource.class))).thenReturn(Result.success(dataSource("ds-1")));
        when(service.update(any())).thenReturn(ServiceResult.notFound("nope"));

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .put()
                .then()
                .statusCode(404);
    }

    @Test
    void request_shouldReturnDataSources() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformer.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(transformer.transform(isA(DataSource.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(service.query(any())).thenReturn(ServiceResult.success(List.of(dataSource("a"), dataSource("b"))));

        baseRequest()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    void get_shouldReturnDataSource() {
        when(service.findById("ds-1")).thenReturn(ServiceResult.success(dataSource("ds-1")));
        when(transformer.transform(isA(DataSource.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add(ID, "ds-1").build()));

        baseRequest()
                .get("/{id}", "ds-1")
                .then()
                .log().ifError()
                .statusCode(200)
                .body("'@id'", is("ds-1"));
    }

    @Test
    void get_shouldReturn404_whenNotFound() {
        when(service.findById("missing")).thenReturn(ServiceResult.notFound("nope"));

        baseRequest()
                .get("/{id}", "missing")
                .then()
                .statusCode(404);
    }

    @Test
    void delete_shouldReturn204() {
        when(service.deleteById("ds-1")).thenReturn(ServiceResult.success());

        baseRequest()
                .delete("/{id}", "ds-1")
                .then()
                .statusCode(204);
    }

    @Test
    void delete_shouldReturn404_whenNotFound() {
        when(service.deleteById("missing")).thenReturn(ServiceResult.notFound("nope"));

        baseRequest()
                .delete("/{id}", "missing")
                .then()
                .statusCode(404);
    }

    @Override
    protected Object controller() {
        return new DataSourceApiV4Controller(service, transformer, validator, monitor);
    }

    private DataSource dataSource(String id) {
        return DataSource.Builder.newInstance().id(id).name("a name").type("HttpData").build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/v4/datasources")
                .when();
    }
}
