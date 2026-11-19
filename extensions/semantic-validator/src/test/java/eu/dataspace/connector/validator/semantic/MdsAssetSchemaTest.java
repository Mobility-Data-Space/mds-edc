package eu.dataspace.connector.validator.semantic;

import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static eu.dataspace.connector.validator.semantic.SemanticValidator.MDS_ASSET_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the bundled schema itself. References are resolved lazily by the validator, so a broken one
 * would only surface when an asset reaches that branch of the schema at runtime.
 */
class MdsAssetSchemaTest {

    private static final String DRAFT_2020_12_META_SCHEMA = "https://json-schema.org/draft/2020-12/schema";

    private final String schema = loadSchema();

    @Test
    void shouldBeValidAgainstMetaSchema() {
        var metaSchema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(SchemaLocation.of(DRAFT_2020_12_META_SCHEMA));

        var errors = metaSchema.validate(schema, InputFormat.JSON);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldResolveAllReferences() {
        var root = Json.createReader(new StringReader(schema)).readObject();
        var refs = new ArrayList<String>();
        collectRefs(root, refs);

        assertThat(refs).isNotEmpty().allSatisfy(ref -> {
            assertThat(ref).as("only local references are supported").startsWith("#");
            assertThat(resolves(root, ref.substring(1))).as("reference %s should resolve", ref).isTrue();
        });
    }

    private static void collectRefs(JsonValue value, List<String> refs) {
        switch (value.getValueType()) {
            case OBJECT -> value.asJsonObject().forEach((key, child) -> {
                if (key.equals("$ref") && child instanceof JsonString ref) {
                    refs.add(ref.getString());
                } else {
                    collectRefs(child, refs);
                }
            });
            case ARRAY -> value.asJsonArray().forEach(child -> collectRefs(child, refs));
            default -> { }
        }
    }

    private static boolean resolves(JsonObject root, String pointer) {
        try {
            return Json.createPointer(pointer).containsValue((JsonStructure) root);
        } catch (JsonException e) {
            return false;
        }
    }

    private static String loadSchema() {
        try (var stream = MdsAssetSchemaTest.class.getClassLoader().getResourceAsStream(MDS_ASSET_SCHEMA)) {
            assertThat(stream).as("schema resource %s", MDS_ASSET_SCHEMA).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
