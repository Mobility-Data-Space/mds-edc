package eu.dataspace.connector.extension.validator.dataaddress.kafka;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;

public class KafkaBrokerDataAddressValidator implements Validator<DataAddress> {

    public ValidationResult validate(DataAddress input) {
//        var violations = Stream.of(
//                TOPIC,
//                BOOTSTRAP_SERVERS,
//                MECHANISM,
//                PROTOCOL,
////                OAUTH_TOKEN_URL,
////                OAUTH_REVOKE_URL,
//                OAUTH_CLIENT_ID,
//                OAUTH_CLIENT_SECRET_KEY
//        ).map((final String it) -> {
//            var value = input.getStringProperty(it);
//            return value != null && !value.isBlank() ? null : Violation.violation("'%s' is a mandatory attribute".formatted(it), it, value);
//        }).filter(Objects::nonNull).toList();
//
//        return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
        // TODO: validation is the very last thing that needs to be implemented
        return ValidationResult.success();
    }
}

