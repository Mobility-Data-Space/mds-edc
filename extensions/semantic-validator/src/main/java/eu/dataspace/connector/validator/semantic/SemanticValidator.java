package eu.dataspace.connector.validator.semantic;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static eu.dataspace.connector.validator.semantic.Vocabulary.Property.property;
import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates Asset properties against a vocabulary
 */
public class SemanticValidator implements Validator<JsonObject> {

    private final JsonLdPath path;
    private final Vocabulary vocabulary;

    public static JsonObjectValidator instance(Vocabulary vocabulary) {
        return JsonObjectValidator.newValidator()
                .verifyObject(EDC_ASSET_PROPERTIES, builder -> builder.verify(path -> new SemanticValidator(path, vocabulary)))
                .build();
    }

    public SemanticValidator(JsonLdPath path, Vocabulary vocabulary) {
        this.path = path;
        this.vocabulary = vocabulary;
    }

    /**
     * Validation is divided in 3 steps:
     * - required properties are set
     * - only allowed properties are set
     * - eventual enum properties have values that are allowed
     *
     * @param jsonObject the array properties
     * @return the validation result.
     */
    @Override
    public ValidationResult validate(JsonObject jsonObject) {
        var tree = createTree(jsonObject);
        var properties = extractProperties(tree).collect(toSet());
        var values = extractValues(tree).collect(toSet());

        var requiredViolations = vocabulary.required().stream()
                .filter(required -> !properties.contains(required))
                .map(this::toRequiredViolation);

        var notAllowedViolations = properties.stream()
                .filter(property -> !vocabulary.allowed().contains(property))
                .map(this::toNotAllowedViolation);

        var enumViolations = vocabulary.enums().entrySet().stream()
                .flatMap(enumItem -> validateEnum(enumItem, values));

        var violations = Stream.of(requiredViolations, notAllowedViolations, enumViolations).flatMap(identity()).toList();
        if (violations.isEmpty()) {
            return ValidationResult.success();
        }

        return ValidationResult.failure(violations);
    }

    private @NotNull Violation toNotAllowedViolation(Vocabulary.Property property) {
        return violation("property '%s' is not allowed".formatted(property.toString()), path.append(property.name()).toString());
    }

    private @NotNull Violation toRequiredViolation(Vocabulary.Property property) {
        return violation("property '%s' is required".formatted(property.toString()), path.append(property.name()).toString());
    }

    private @NotNull Stream<Violation> validateEnum(Map.Entry<String, Set<Vocabulary.Enum>> enumItem, Set<AssetPropertyValue> values) {
        var value = findValue(enumItem.getKey(), values);
        if (value == null) {
            return Stream.empty();
        }

        var enums = enumItem.getValue();
        var enumValue = enums.stream().filter(it -> it.id().equals(value.primitiveValue())).findAny().orElse(null);
        if (enumValue == null) {
            var allowedValues = enums.stream().map(Vocabulary.Enum::id).collect(toSet());
            return Stream.of(violation("enum '%s' should be one of %s but was '%s'"
                    .formatted(enumItem.getKey(), allowedValues, value.primitiveValue()), path.append(enumItem.getKey()).toString()));
        }

        return enumValue.sub().entrySet().stream().flatMap(sub -> validateEnum(sub, values));
    }

    private AssetPropertyValue findValue(String key, Set<AssetPropertyValue> values) {
        return values.stream().filter(it -> it.name().equals(key)).findAny().orElse(null);
    }

    private Stream<AssetPropertyValue> extractValues(Set<AssetProperty> properties) {
        return properties.stream()
                .flatMap(property -> switch (property) {
                    case AssetPropertyValue value -> Stream.of(value);
                    case AssetPropertyChildren children -> extractValues(children.children());
                });
    }

    private Stream<Vocabulary.Property> extractProperties(Set<AssetProperty> assetProperties) {
        return assetProperties.stream()
                .flatMap(assetProperty -> switch (assetProperty) {
                    case AssetPropertyValue value -> Stream.of(property(value.name(), null));
                    case AssetPropertyChildren(var name, var children) -> extractProperties(children).map(child -> property(name, child));
                });
    }

    private Set<AssetProperty> createTree(JsonObject jsonObject) {
        return jsonObject.entrySet().stream()
                .map(entry -> {
                    var array = entry.getValue().asJsonArray();
                    if (array.stream().anyMatch(item -> item.asJsonObject().containsKey(VALUE))) {
                        var value = array.size() == 1
                                ? array.getFirst().asJsonObject().get(VALUE)
                                : array.stream().map(v -> v.asJsonObject().get(VALUE)).collect(toJsonArray());

                        return new AssetPropertyValue(entry.getKey(), value);

                    } else {
                        var children = array.stream().map(it -> createTree(it.asJsonObject()))
                                .flatMap(Collection::stream)
                                .collect(toSet());
                        return new AssetPropertyChildren(entry.getKey(), children);
                    }
                })
                .collect(toSet());
    }

    record AssetPropertyChildren(String name, Set<AssetProperty> children) implements AssetProperty {}

    record AssetPropertyValue(String name, JsonValue value) implements AssetProperty {

        public Object primitiveValue() {
            if (value instanceof JsonString string) {
                return string.getString();
            }
            return value;
        }
    }

    sealed interface AssetProperty permits AssetPropertyValue, AssetPropertyChildren {}

}
