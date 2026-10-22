package eu.dataspace.connector.datasource.api.transform;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_PROPERTIES;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDataSourceTransformerTest {

    private final JsonObjectToDataSourceTransformer transformer = new JsonObjectToDataSourceTransformer();

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Object.class))).thenReturn("http://backend");

        var json = Json.createObjectBuilder()
                .add(ID, "ds-1")
                .add(DATA_SOURCE_NAME, "a name")
                .add(DATA_SOURCE_TYPE_PROPERTY, "HttpData")
                .add(DATA_SOURCE_PROPERTIES, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("https://w3id.org/edc/v0.0.1/ns/baseUrl", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder().add(VALUE, "http://backend")))))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("ds-1");
        assertThat(result.getName()).isEqualTo("a name");
        assertThat(result.getType()).isEqualTo("HttpData");
        assertThat(result.getProperties()).containsEntry("https://w3id.org/edc/v0.0.1/ns/baseUrl", "http://backend");
    }
}
