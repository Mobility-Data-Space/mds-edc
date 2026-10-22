package eu.dataspace.connector.datasource.api.transform;

import eu.dataspace.connector.datasource.spi.types.DataSource;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_PROPERTIES;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_PROPERTY;

/**
 * Converts a {@link DataSource} in JSON-LD expanded form into a {@link DataSource}. The {@code properties} are read as
 * opaque generic values, keyed by their expanded (full-IRI) term.
 */
public class JsonObjectToDataSourceTransformer extends AbstractJsonLdTransformer<JsonObject, DataSource> {

    public JsonObjectToDataSourceTransformer() {
        super(JsonObject.class, DataSource.class);
    }

    @Override
    public @Nullable DataSource transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataSource.Builder.newInstance()
                .id(nodeId(jsonObject));

        visitProperties(jsonObject, key -> switch (key) {
            case DATA_SOURCE_NAME -> value -> builder.name(transformString(value, context));
            case DATA_SOURCE_TYPE_PROPERTY -> value -> builder.type(transformString(value, context));
            case DATA_SOURCE_PROPERTIES -> value ->
                    visitProperties(value.asJsonArray().getJsonObject(0),
                            (k, v) -> builder.property(k, transformGenericProperty(v, context)));
            default -> doNothing();
        });

        return builderResult(builder::build, context);
    }
}
