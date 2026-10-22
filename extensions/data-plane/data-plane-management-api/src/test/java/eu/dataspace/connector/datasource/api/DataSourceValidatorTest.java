package eu.dataspace.connector.datasource.api;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_PROPERTY;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

class DataSourceValidatorTest {

    private final org.eclipse.edc.validator.spi.Validator<jakarta.json.JsonObject> validator = DataSourceValidator.instance();

    @Test
    void shouldPass_whenNameAndTypePresent() {
        var input = Json.createObjectBuilder()
                .add(DATA_SOURCE_NAME, value("a name"))
                .add(DATA_SOURCE_TYPE_PROPERTY, value("HttpData"))
                .build();

        assertThat(validator.validate(input)).isSucceeded();
    }

    @Test
    void shouldFail_whenNameMissing() {
        var input = Json.createObjectBuilder()
                .add(DATA_SOURCE_TYPE_PROPERTY, value("HttpData"))
                .build();

        assertThat(validator.validate(input)).isFailed();
    }

    @Test
    void shouldFail_whenTypeMissing() {
        var input = Json.createObjectBuilder()
                .add(DATA_SOURCE_NAME, value("a name"))
                .build();

        assertThat(validator.validate(input)).isFailed();
    }

    private jakarta.json.JsonArrayBuilder value(String value) {
        return Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, value));
    }
}
