package eu.dataspace.connector.validator.semantic;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.Json.createValue;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class SemanticValidatorTest {

    private static final String DCT = "http://purl.org/dc/terms/";
    private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
    private static final String MOBILITY = "https://w3id.org/mobilitydcat-ap/";
    private static final String THEME = "https://w3id.org/mobilitydcat-ap/mobility-theme/";
    private static final String EDC = "https://w3id.org/edc/v0.0.1/ns/";

    private static final String TITLE = DCT + "title";
    private static final String MOBILITY_THEME = MOBILITY + "mobilityTheme";
    private static final String CATEGORY = THEME + "data-content-category";
    private static final String SUB_CATEGORY = THEME + "data-content-sub-category";

    private final SemanticValidator validator = SemanticValidator.mdsAsset();

    @Test
    void shouldSucceed_whenRequiredPropertiesAreSet() {
        var result = validator.validate(asset(title(), theme("OTHER")));

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenPropertiesAreMissing() {
        var result = validator.validate(createObjectBuilder().add(ID, "asset-id").build());

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenTitleIsMissing() {
        var result = validator.validate(asset(theme("OTHER")));

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                .hasSize(1).extracting(Violation::message).first().asString().contains(TITLE));
    }

    @Test
    void shouldFail_whenCategoryIsMissing() {
        var result = validator.validate(asset(title(), entry(MOBILITY_THEME, node(entry(SUB_CATEGORY, literal("any"))))));

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                .hasSize(1).extracting(Violation::message).first().asString().contains(CATEGORY));
    }

    @Test
    void shouldFail_whenPropertyIsNotAllowed() {
        var result = validator.validate(asset(title(), theme("OTHER"), entry(EDC + "unsupported", literal("any"))));

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                .hasSize(1).extracting(Violation::message).first().asString().contains("unsupported"));
    }

    @Test
    void shouldFail_whenNestedPropertyIsNotAllowed() {
        Map.Entry<String, JsonValue> spatial = entry(DCT + "spatial", node(entry(SKOS + "prefLabel", literal("label")), entry(DCT + "unsupported", literal("any"))));

        var result = validator.validate(asset(title(), theme("OTHER"), spatial));

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                .hasSize(1).first().satisfies(violation -> {
                    assertThat(violation.message()).contains("unsupported");
                    assertThat(violation.path()).contains(DCT + "spatial");
                }));
    }

    @Test
    void shouldFail_whenLiteralPropertyIsAnObject() {
        var result = validator.validate(asset(title(), theme("OTHER"), entry(DCT + "publisher", node(entry(ID, createValue("https://publisher"))))));

        assertThat(result).isFailed();
    }

    @Test
    void shouldSucceed_whenNestedIdIsSet() {
        Map.Entry<String, JsonValue> dataStandard = entry(MOBILITY + "mobilityDataStandard", node(entry(ID, createValue("my-data-model"))));

        var result = validator.validate(asset(title(), theme("OTHER"), dataStandard));

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenMultiValuedLiteral() {
        var result = validator.validate(asset(title(), theme("OTHER"), entry("http://www.w3.org/ns/dcat#keywords", literal("one", "two"))));

        assertThat(result).isSucceeded();
    }

    @Nested
    class Enums {

        @Test
        void shouldSucceed_whenTransportModeIsValid() {
            var result = validator.validate(asset(title(), theme("OTHER"), entry(MOBILITY + "transportMode", literal("BICYCLE"))));

            assertThat(result).isSucceeded();
        }

        @Test
        void shouldFail_whenTransportModeIsNotValid() {
            var result = validator.validate(asset(title(), theme("OTHER"), entry(MOBILITY + "transportMode", literal("SPACESHIP"))));

            assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                    .hasSize(1).first().satisfies(violation -> {
                        assertThat(violation.path()).contains("transportMode");
                        assertThat(violation.message()).contains("BICYCLE");
                    }));
        }

        @Test
        void shouldFail_whenEnumHasMultipleValues() {
            var result = validator.validate(asset(title(), theme("OTHER"), entry(MOBILITY + "transportMode", literal("BUS", "CAR"))));

            assertThat(result).isFailed();
        }

        @Test
        void shouldFail_whenCategoryIsNotValid() {
            var result = validator.validate(asset(title(), theme("UNKNOWN")));

            assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                    .hasSize(1).first().satisfies(violation -> assertThat(violation.path()).contains("data-content-category")));
        }

        @Test
        void shouldSucceed_whenSubCategoryBelongsToCategory() {
            var result = validator.validate(asset(title(), theme("FILLING_AND_CHARGING_STATIONS", "AVAILABILITY_OF_CHARGING_POINTS_FOR_ELECTRIC_VEHICLES")));

            assertThat(result).isSucceeded();
        }

        @Test
        void shouldFail_whenSubCategoryDoesNotBelongToCategory() {
            var result = validator.validate(asset(title(), theme("FILLING_AND_CHARGING_STATIONS", "LONG_TERM_ROAD_WORKS")));

            assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                    .hasSize(1).first().satisfies(violation -> assertThat(violation.path()).contains("data-content-sub-category")));
        }

        @Test
        void shouldFail_whenSubCategoryIsSetForCategoryWithoutSubCategories() {
            var result = validator.validate(asset(title(), theme("OTHER", "LONG_TERM_ROAD_WORKS")));

            assertThat(result).isFailed();
        }

        @Test
        void shouldSucceed_whenCategoryAndSubCategoryAreSplitInDifferentNodes() {
            var result = validator.validate(asset(title(), splitTheme("FILLING_AND_CHARGING_STATIONS", "AVAILABILITY_OF_FILLING_STATIONS")));

            assertThat(result).isSucceeded();
        }

        @Test
        void shouldFail_whenSplitSubCategoryDoesNotBelongToCategory() {
            var result = validator.validate(asset(title(), splitTheme("FILLING_AND_CHARGING_STATIONS", "LONG_TERM_ROAD_WORKS")));

            assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                    .hasSize(1).first().satisfies(violation -> {
                        assertThat(violation.path()).contains("data-content-sub-category");
                        assertThat(violation.message()).contains("AVAILABILITY_OF_FILLING_STATIONS");
                    }));
        }

        @Test
        void shouldFail_whenSplitSubCategoryIsSetForCategoryWithoutSubCategories() {
            var result = validator.validate(asset(title(), splitTheme("OTHER", "LONG_TERM_ROAD_WORKS")));

            assertThat(result).isFailed();
        }

        @Test
        void shouldSucceed_whenMultipleValidSubCategoriesAreSet() {
            var themes = createArrayBuilder()
                    .add(createObjectBuilder().add(CATEGORY, literal("FILLING_AND_CHARGING_STATIONS")))
                    .add(createObjectBuilder().add(SUB_CATEGORY, literal("AVAILABILITY_OF_FILLING_STATIONS")))
                    .add(createObjectBuilder().add(SUB_CATEGORY, literal("LOCATION_AND_CONDITIONS_OF_FILLING_STATIONS")))
                    .build();

            var result = validator.validate(asset(title(), entry(MOBILITY_THEME, themes)));

            assertThat(result).isSucceeded();
        }

        @Test
        void shouldFail_whenOneOfMultipleCategoriesIsNotValid() {
            var themes = createArrayBuilder()
                    .add(createObjectBuilder().add(CATEGORY, literal("OTHER")))
                    .add(createObjectBuilder().add(CATEGORY, literal("UNKNOWN")))
                    .build();

            var result = validator.validate(asset(title(), entry(MOBILITY_THEME, themes)));

            assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                    .hasSize(1).first().satisfies(violation -> assertThat(violation.path()).contains("data-content-category")));
        }

        @Test
        void shouldFail_whenSubCategoryDoesNotBelongToEveryCategory() {
            var themes = createArrayBuilder()
                    .add(createObjectBuilder().add(CATEGORY, literal("TOLL_INFORMATION")))
                    .add(createObjectBuilder().add(CATEGORY, literal("FREIGHT_AND_LOGISTICS")))
                    .add(createObjectBuilder().add(SUB_CATEGORY, literal("IDENTIFICATION_OF_TOLLED_ROADS")))
                    .build();

            var result = validator.validate(asset(title(), entry(MOBILITY_THEME, themes)));

            assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations())
                    .hasSize(1).first().satisfies(violation -> assertThat(violation.message()).contains("LOCATION_OF_DELIVERY_AREAS")));
        }

        @Test
        void shouldSucceed_whenMultipleValidCategoriesAreSet() {
            var themes = createArrayBuilder()
                    .add(createObjectBuilder().add(CATEGORY, literal("OTHER")))
                    .add(createObjectBuilder().add(CATEGORY, literal("TOLL_INFORMATION")))
                    .build();

            var result = validator.validate(asset(title(), entry(MOBILITY_THEME, themes)));

            assertThat(result).isSucceeded();
        }
    }

    private Map.Entry<String, JsonValue> title() {
        return entry(TITLE, literal("My Asset"));
    }

    private Map.Entry<String, JsonValue> theme(String category) {
        return entry(MOBILITY_THEME, node(entry(CATEGORY, literal(category))));
    }

    private Map.Entry<String, JsonValue> theme(String category, String subCategory) {
        return entry(MOBILITY_THEME, node(entry(CATEGORY, literal(category)), entry(SUB_CATEGORY, literal(subCategory))));
    }

    private Map.Entry<String, JsonValue> splitTheme(String category, String subCategory) {
        return entry(MOBILITY_THEME, createArrayBuilder()
                .add(createObjectBuilder().add(CATEGORY, literal(category)))
                .add(createObjectBuilder().add(SUB_CATEGORY, literal(subCategory)))
                .build());
    }

    @SafeVarargs
    private JsonObject asset(Map.Entry<String, JsonValue>... properties) {
        return createObjectBuilder()
                .add(ID, "asset-id")
                .add(EDC_ASSET_PROPERTIES, node(properties))
                .build();
    }

    @SafeVarargs
    private JsonArray node(Map.Entry<String, JsonValue>... properties) {
        var object = createObjectBuilder();
        for (var property : properties) {
            object.add(property.getKey(), property.getValue());
        }
        return createArrayBuilder().add(object).build();
    }

    private JsonArray literal(String... values) {
        var array = createArrayBuilder();
        for (var value : values) {
            array.add(createObjectBuilder().add(VALUE, value));
        }
        return array.build();
    }

}
