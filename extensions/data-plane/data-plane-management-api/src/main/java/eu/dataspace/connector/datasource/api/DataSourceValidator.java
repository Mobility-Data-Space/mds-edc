package eu.dataspace.connector.datasource.api;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE_PROPERTY;

/**
 * Validates the create/update body of a {@code DataSource}: {@code name} and {@code type} are mandatory, the opaque
 * {@code properties} are not constrained.
 */
public class DataSourceValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(DATA_SOURCE_NAME, MandatoryValue::new)
                .verify(DATA_SOURCE_TYPE_PROPERTY, MandatoryValue::new)
                .build();
    }
}
