package eu.dataspace.connector.validator.semantic;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static eu.dataspace.connector.validator.semantic.Vocabulary.Enum.enumProperty;
import static eu.dataspace.connector.validator.semantic.Vocabulary.Property.property;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class SemanticValidatorTest {

    private final Vocabulary vocabulary = new Vocabulary(
            Set.of(property("required")),
            Set.of(
                    property("optional"),
                    property("nested", property("optional")),
                    property("nested", property("@id")),
                    property("simpleEnum"),
                    property("nested", property("firstEnum")),
                    property("nested", property("secondEnum"))
            ),
            Map.of(
                    "simpleEnum", Set.of(enumProperty("ENUM_VALUE")),
                    "firstEnum", Set.of(enumProperty("FIRST_VALUE", Map.of(
                            "secondEnum", Set.of(enumProperty("SECOND_VALUE"))))
                    )
            )
    );
    private final JsonObjectValidator validator = SemanticValidator.instance(vocabulary);

    @Test
    void shouldSucceed_whenContainsRequiredProperties() {
        var input = assetWithProperties(entry("required", value("any")));

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenRequiredPropertiesAreNotSet() {
        var input = assetWithProperties();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).extracting(Violation::message)
                    .anySatisfy(message -> assertThat(message).contains("[required]"));
        });
    }

    @Test
    void shouldFail_whenUnallowedPropertyIsSet() {
        var input = assetWithProperties(
                entry("required", value("any")),
                entry("optional", value("any")),
                entry("nested", sub("optional", value("any"))),
                entry("unsupported", value("any"))
        );

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).extracting(Violation::message)
                    .anySatisfy(message -> assertThat(message).contains("unsupported"));
        });
    }

    @Test
    void shouldPass_whenIdNestedProperty() {
        var input = assetWithProperties(
                entry("required", value("any")),
                entry("nested", sub("@id", Json.createValue("id")))
        );

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenUnallowedNestedPropertyIsSet() {
        var input = assetWithProperties(
                entry("required", value("any")),
                entry("nested", sub("unallowed", sub("property", value("any"))))
        );

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).extracting(Violation::message)
                    .anySatisfy(message -> assertThat(message).contains("[nested].[unallowed].[property]"));
        });
    }

    @Test
    void shouldSucceed_whenEnumTypeSetWithExpectedValue() {
        var input = assetWithProperties(
                entry("required", value("any")),
                entry("simpleEnum", value("ENUM_VALUE"))
        );

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenEnumTypeNotSetWithExpectedValue() {
        var input = assetWithProperties(
                entry("required", value("any")),
                entry("simpleEnum", value("unexpected"))
        );

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).extracting(Violation::message)
                    .anySatisfy(message -> assertThat(message).contains("simpleEnum").contains("unexpected").contains("ENUM_VALUE"));
        });
    }

    @Test
    void shouldFail_whenEnumNestedTypeNotSetWithExpectedValue() {
        var input = assetWithProperties(
                entry("required", value("any")),
                entry("nested", Json.createArrayBuilder()
                        .add(createObjectBuilder().add("firstEnum", value("FIRST_VALUE")))
                        .add(createObjectBuilder().add("secondEnum", value("unexpected")))
                        .build()
                )
        );

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).extracting(Violation::message)
                    .anySatisfy(message -> assertThat(message)
                            .contains("secondEnum")
                            .contains("SECOND_VALUE")
                            .contains("unexpected")
                    );
        });
    }

    @SafeVarargs
    private JsonObject assetWithProperties(Map.Entry<String, JsonValue>... properties) {
        var jsonProperties = createObjectBuilder();

        for (var property : properties) {
            jsonProperties.add(property.getKey(), property.getValue());
        }

        return createObjectBuilder().add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(jsonProperties)).build();
    }

    private JsonArray sub(String key, JsonValue value) {
        return createArrayBuilder().add(createObjectBuilder().add(key, value)).build();
    }

    private JsonArray value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value)).build();
    }

}
