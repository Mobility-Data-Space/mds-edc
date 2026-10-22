package eu.dataspace.connector.datasource.api.transform;

import eu.dataspace.connector.datasource.spi.types.DataSource;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_CREATED_AT;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_PROPERTIES;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_PROPERTY;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromDataSourceTransformerTest {

    private final JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());
    private final TypeManager typeManager = mock();
    private final JsonObjectFromDataSourceTransformer transformer =
            new JsonObjectFromDataSourceTransformer(factory, typeManager, JSON_LD);

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        when(typeManager.getMapper(JSON_LD)).thenReturn(JacksonJsonLd.createObjectMapper());

        var dataSource = DataSource.Builder.newInstance()
                .id("ds-1")
                .name("a name")
                .type("HttpData")
                .property("https://w3id.org/edc/v0.0.1/ns/baseUrl", "http://backend")
                .createdAt(123L)
                .updatedAt(456L)
                .build();

        var result = transformer.transform(dataSource, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("ds-1");
        assertThat(result.getString(TYPE)).isEqualTo(DATA_SOURCE_TYPE);
        assertThat(result.getString(DATA_SOURCE_NAME)).isEqualTo("a name");
        assertThat(result.getString(DATA_SOURCE_TYPE_PROPERTY)).isEqualTo("HttpData");
        assertThat(result.getJsonNumber(DATA_SOURCE_CREATED_AT).longValue()).isEqualTo(123L);
        assertThat(result.getJsonNumber(DATA_SOURCE_UPDATED_AT).longValue()).isEqualTo(456L);
        assertThat(result.getJsonObject(DATA_SOURCE_PROPERTIES).getString("https://w3id.org/edc/v0.0.1/ns/baseUrl"))
                .isEqualTo("http://backend");
        verify(context, never()).reportProblem(anyString());
    }
}
