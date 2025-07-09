package eu.dataspace.connector.validator.semantic;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;

import static eu.dataspace.connector.validator.semantic.Vocabulary.Enum.enumProperty;
import static eu.dataspace.connector.validator.semantic.Vocabulary.Property.property;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

/**
 * Provides a Vocabulary that can be semantically validated
 */
public class VocabularyProvider {

    private static final String VOCABULARY_FILE = "mds-vocabulary.json";

    /**
     * Load the vocabulary from a resource file.
     *
     * @return the vocabulary
     */
    public Vocabulary provide() {
        var vocabularyFile = getClass().getClassLoader().getResource(VOCABULARY_FILE);
        if (vocabularyFile == null) {
            throw new RuntimeException("Cannot load vocabulary from %s: resource not found".formatted(VOCABULARY_FILE));
        }

        try (var stream = vocabularyFile.openStream()) {
            var reader = Json.createReader(stream);
            return toVocabulary(reader.readObject());
        } catch (IOException e) {
            throw new RuntimeException("Cannot deserialize %s".formatted(VOCABULARY_FILE), e);
        }
    }

    private Vocabulary toVocabulary(JsonObject ontology) {
        var namespaces = ontology.getJsonObject("jsonld-context").entrySet().stream().collect(toMap(Map.Entry::getKey, it -> ((JsonString)it.getValue()).getString()));

        var required = ontology.getJsonArray("required").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(it -> createProperty(it, namespaces))
                .collect(toSet());

        var optional = ontology.getJsonArray("optional").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(it -> createProperty(it, namespaces))
                .collect(toSet());

        var enums = ontology.getJsonObject("enums").entrySet().stream().collect(toEnum(namespaces));

        return new Vocabulary(required, optional, enums);
    }

    private Vocabulary.Enum getEnum(JsonValue value, Map<String, @NotNull String> namespaces) {
        if (value instanceof JsonString stringSubEntry) {
            return enumProperty(stringSubEntry.getString());
        }

        if (value instanceof JsonObject objectSubEntry) {
            var sub = objectSubEntry.entrySet().stream()
                    .filter(entry -> !"id".equals(entry.getKey()))
                    .filter(entry -> entry.getValue().getValueType().equals(JsonValue.ValueType.ARRAY))
                    .collect(toEnum(namespaces));
            return enumProperty(objectSubEntry.getString("id"), sub);
        }

        return null;
    }

    private @NotNull Collector<Map.Entry<String, JsonValue>, ?, Map<String, Set<Vocabulary.Enum>>> toEnum(Map<String, String> namespaces) {
        return toMap(
                entry -> expand(entry.getKey(), namespaces),
                entry -> entry.getValue().asJsonArray().stream()
                        .map(it -> getEnum(it, namespaces))
                        .filter(Objects::nonNull)
                        .collect(toSet())
        );
    }

    private Vocabulary.Property createProperty(String text, Map<String, String> namespaces) {
        var parts = text.split("\\.", 2);
        if (parts.length == 1) {
            return property(expand(parts[0], namespaces));
        } else {
            return property(expand(parts[0], namespaces), createProperty(parts[1], namespaces));
        }
    }

    private String expand(String value, Map<String, String> namespaces) {
        var tokens = value.split(":");
        if (Objects.equals(tokens[0], ID)) {
            return ID;
        }

        var url = namespaces.get(tokens[0]);
        if (tokens.length == 1) {
            return url;
        } else {
            return url + tokens[1];
        }
    }
}
