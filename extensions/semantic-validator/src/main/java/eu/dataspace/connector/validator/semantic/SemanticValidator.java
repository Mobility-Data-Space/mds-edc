package eu.dataspace.connector.validator.semantic;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.PathType;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.io.IOException;

import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates an expanded JSON-LD Asset against a JSON Schema.
 */
public class SemanticValidator implements Validator<JsonObject> {

    public static final String MDS_ASSET_SCHEMA = "mds-asset-schema.json";

    private final Schema schema;

    public SemanticValidator(Schema schema) {
        this.schema = schema;
    }

    /**
     * Create a validator using the MDS asset schema bundled as resource.
     */
    public static SemanticValidator mdsAsset() {
        try (var stream = SemanticValidator.class.getClassLoader().getResourceAsStream(MDS_ASSET_SCHEMA)) {
            if (stream == null) {
                throw new IllegalStateException("Cannot load JSON schema from %s: resource not found".formatted(MDS_ASSET_SCHEMA));
            }
            var config = SchemaRegistryConfig.builder().pathType(PathType.JSON_PATH).build();
            var schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12, builder -> builder.schemaRegistryConfig(config))
                    .getSchema(stream);
            return new SemanticValidator(schema);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read JSON schema %s".formatted(MDS_ASSET_SCHEMA), e);
        }
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var errors = schema.validate(input.toString(), InputFormat.JSON);
        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors.stream()
                .map(error -> violation(error.getMessage(), error.getInstanceLocation().toString()))
                .toList());
    }

}
