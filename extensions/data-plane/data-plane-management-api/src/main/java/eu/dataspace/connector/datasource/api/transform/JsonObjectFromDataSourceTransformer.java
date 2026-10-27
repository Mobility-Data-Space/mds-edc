package eu.dataspace.connector.datasource.api.transform;

import eu.dataspace.connector.datasource.spi.types.DataSource;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdFromModelTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_CREATED_AT;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_PROPERTIES;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_PROPERTY;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_UPDATED_AT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

/**
 * Converts a {@link DataSource} into its JSON-LD representation. The opaque {@code properties} are serialized as a
 * nested object keyed by their (expanded) term, which compaction shortens back on the way out.
 */
public class JsonObjectFromDataSourceTransformer extends JsonLdFromModelTransformer<DataSource, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDataSourceTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(DataSource.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataSource dataSource, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, dataSource.getId())
                .add(TYPE, DATA_SOURCE_TYPE)
                .add(DATA_SOURCE_NAME, dataSource.getName())
                .add(DATA_SOURCE_TYPE_PROPERTY, dataSource.getType())
                .add(DATA_SOURCE_CREATED_AT, dataSource.getCreatedAt())
                .add(DATA_SOURCE_UPDATED_AT, dataSource.getUpdatedAt());

        var propertiesBuilder = jsonFactory.createObjectBuilder();
        transformProperties(dataSource.getProperties(), propertiesBuilder, typeManager.getMapper(typeContext), context);
        builder.add(DATA_SOURCE_PROPERTIES, propertiesBuilder);

        return builder.build();
    }
}
