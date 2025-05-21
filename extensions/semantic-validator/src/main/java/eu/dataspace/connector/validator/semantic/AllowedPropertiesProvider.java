package eu.dataspace.connector.validator.semantic;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLd;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;

public class AllowedPropertiesProvider {

    private static final String EXAMPLE_FILE = "mds-asset-example-properties.json";
    private final JsonLd jsonLd;

    public AllowedPropertiesProvider(JsonLd jsonLd) {
        this.jsonLd = jsonLd;
    }

    public Set<String> provide() {
        var exampleFile = getClass().getClassLoader().getResource(EXAMPLE_FILE);
        if (exampleFile == null) {
            throw new RuntimeException("Cannot extract allowed properties key from %s: resource not found".formatted(EXAMPLE_FILE));
        }

        try (var stream = exampleFile.openStream()) {
            var reader = Json.createReader(stream);
            var expansion = jsonLd.expand(reader.readObject());
            if (expansion.failed()) {
                throw new RuntimeException("Cannot extract allowed properties key from %s: json-ld expansion failed: %s".formatted(EXAMPLE_FILE, expansion.getFailureDetail()));
            }
            return expansion.getContent()
                    .getJsonArray(EDC_ASSET_PROPERTIES).stream()
                    .map(JsonValue::asJsonObject)
                    .map(Map::keySet)
                    .flatMap(Collection::stream)
                    .collect(toSet());
        } catch (IOException e) {
            throw new RuntimeException("Cannot extract allowed properties key from %s".formatted(EXAMPLE_FILE), e);
        }

    }

}
