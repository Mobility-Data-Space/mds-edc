package eu.dataspace.connector.extension.negotiation.manual.approval.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

/**
 * this is a workaround for this bug: https://github.com/eclipse-edc/Connector/issues/4955 it can be removed when it
 * will be fixed (likely in version 0.13.0)
 */
@Deprecated
public class JsonValueToGenericTypeTransformer extends AbstractJsonLdTransformer<JsonValue, Object> {
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonValueToGenericTypeTransformer(TypeManager typeManager, String typeContext) {
        super(JsonValue.class, Object.class);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public Object transform(@NotNull JsonValue value, @NotNull TransformerContext context) {
        if (value instanceof JsonObject object) {
            if (object.containsKey(VALUE)) {
                var valueField = object.get(VALUE);
                if (valueField == null) {
                    // parse it as a generic object type
                    return toJavaType(object, context);
                }
                return transform(valueField, context);
            } else {
                return toJavaType(object, context);
            }
        } else if (value instanceof JsonArray jsonArray) {
            return jsonArray.stream().map(entry -> transform(entry, context)).collect(toList());
        } else if (value instanceof JsonString jsonString) {
            return jsonString.getString();
        } else if (value instanceof JsonNumber jsonNumber) {
            return jsonNumber.doubleValue(); // use to double to avoid loss of precision
        } else {
            if (value.getValueType() == JsonValue.ValueType.FALSE) {
                return Boolean.FALSE;
            } else if (value.getValueType() == JsonValue.ValueType.TRUE) {
                return Boolean.TRUE;
            }
        }
        return null;
    }

    private Object toJavaType(JsonObject object, TransformerContext context) {
        try {
            return typeManager.getMapper(typeContext).readValue(object.toString(), Object.class);
        } catch (JsonProcessingException e) {
            context.reportProblem(format("Failed to read value: %s", e.getMessage()));
            return null;
        }
    }

}
