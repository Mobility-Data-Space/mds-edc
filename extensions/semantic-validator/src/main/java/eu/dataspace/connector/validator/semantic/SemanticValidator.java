package eu.dataspace.connector.validator.semantic;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Set;

import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class SemanticValidator implements Validator<JsonObject> {

    private final JsonLdPath path;
    private final Set<String> allowed;

    public static JsonObjectValidator instance(Set<String> allowed) {
        return JsonObjectValidator.newValidator()
                .verifyObject(EDC_ASSET_PROPERTIES, builder -> builder.verify(path -> new SemanticValidator(path, allowed)))
                .build();
    }

    public SemanticValidator(JsonLdPath path, Set<String> allowed) {
        this.path = path;
        this.allowed = allowed;
    }

    @Override
    public ValidationResult validate(JsonObject jsonObject) {
        var violations = jsonObject.asJsonObject().keySet().stream().filter(key -> !allowed.contains(key))
                .map(key -> violation("Asset property '%s' is not allowed".formatted(key), path.append(key).toString()))
                .toList();

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }

        return ValidationResult.failure(violations);
    }

}
