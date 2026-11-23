package eu.dataspace.connector.validator.semantic;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.dataspace.connector.validator.semantic.SemanticValidator.MDS_ASSET_SCHEMA;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the enumerations listed in the vocabulary documentation match the bundled schema.
 * Run with {@code -PupdateDocs} to regenerate the documentation sections instead.
 */
class VocabularyDocumentationTest {

    private static final Path DOCUMENTATION = Path.of("../../docs/usage/mds_features/mds_vocabulary_semantics.md");
    private static final String UPDATE_PROPERTY = "updateDocs";

    private static final String MOBILITY = "https://w3id.org/mobilitydcat-ap/";
    private static final String CATEGORY = MOBILITY + "mobility-theme/data-content-category";
    private static final String SUB_CATEGORY = MOBILITY + "mobility-theme/data-content-sub-category";

    private final JsonObject assetProperties = loadSchema().getJsonObject("$defs").getJsonObject("assetProperties").getJsonObject("properties");

    @Test
    void shouldListSchemaEnumerations() throws IOException {
        var documentation = Files.readString(DOCUMENTATION);

        var expected = replaceSection(replaceSection(documentation, "categories", categories()), "transport-modes", transportModes());

        if (System.getProperty(UPDATE_PROPERTY) != null) {
            Files.writeString(DOCUMENTATION, expected);
            return;
        }

        assertThat(documentation)
                .as("%s is out of sync with %s, run `./gradlew :extensions:semantic-validator:test -PupdateDocs` to update it", DOCUMENTATION.getFileName(), MDS_ASSET_SCHEMA)
                .isEqualTo(expected);
    }

    private String categories() {
        var subCategories = subCategoriesByCategory();
        return enumValues(assetProperties.getJsonObject(MOBILITY + "mobilityTheme").getJsonObject("items").getJsonObject("properties").getJsonObject(CATEGORY))
                .stream()
                .map(category -> {
                    assertThat(subCategories).as("sub-category constraint for category %s", category).containsKey(category);
                    var categorySubCategories = subCategories.get(category);
                    if (categorySubCategories.isEmpty()) {
                        return "- `%s` (no sub-categories)".formatted(category);
                    }
                    return "- `%s`\n%s".formatted(category, categorySubCategories.stream().map("  - `%s`"::formatted).collect(joining("\n")));
                })
                .collect(joining("\n"));
    }

    private String transportModes() {
        return enumValues(assetProperties.getJsonObject(MOBILITY + "transportMode")).stream()
                .map("- `%s`"::formatted)
                .collect(joining("\n"));
    }

    /**
     * Every category is bound to its sub-categories by an {@code if}/{@code then} entry of the mobility theme {@code allOf}.
     */
    private Map<String, List<String>> subCategoriesByCategory() {
        var result = new LinkedHashMap<String, List<String>>();
        assetProperties.getJsonObject(MOBILITY + "mobilityTheme").getJsonArray("allOf").stream()
                .map(JsonValue::asJsonObject)
                .filter(condition -> condition.containsKey("if"))
                .forEach(condition -> {
                    var category = condition.getJsonObject("if").getJsonObject("contains").getJsonObject("properties")
                            .getJsonObject(CATEGORY).getJsonObject("items").getJsonObject("properties")
                            .getJsonObject("@value").getString("const");
                    var subCategory = condition.getJsonObject("then").getJsonObject("items").getJsonObject("properties").get(SUB_CATEGORY);
                    var subCategories = subCategory.getValueType() == JsonValue.ValueType.OBJECT ? enumValues(subCategory.asJsonObject()) : List.<String>of();
                    result.put(category, subCategories);
                });
        return result;
    }

    private static List<String> enumValues(JsonObject singleLiteral) {
        return singleLiteral.getJsonObject("items").getJsonObject("properties").getJsonObject("@value").getJsonArray("enum")
                .getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .toList();
    }

    private static String replaceSection(String documentation, String name, String content) {
        var pattern = Pattern.compile("(<!-- generated:%1$s:start[^>]*-->\n)(?s:.*?)(<!-- generated:%1$s:end -->)".formatted(Pattern.quote(name)));
        var matcher = pattern.matcher(documentation);
        assertThat(matcher.find()).as("generated section '%s' markers in %s", name, DOCUMENTATION).isTrue();
        return matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + "\n" + content + "\n\n" + matcher.group(2)));
    }

    private static JsonObject loadSchema() {
        try (InputStream stream = VocabularyDocumentationTest.class.getClassLoader().getResourceAsStream(MDS_ASSET_SCHEMA)) {
            assertThat(stream).as("schema resource %s", MDS_ASSET_SCHEMA).isNotNull();
            return Json.createReader(stream).readObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
