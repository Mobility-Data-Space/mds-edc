package eu.dataspace.connector.extension.validator.dataaddress.kafka;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;

class KafkaBrokerDataAddressValidatorTest {

    private final KafkaBrokerDataAddressValidator validator = new KafkaBrokerDataAddressValidator();

    @Test
    void shouldPass_whenDataAddressIsValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("Kafka")
                .property(TOPIC, "topic.name")
                .property(BOOTSTRAP_SERVERS, "any:98123")
                .property(MECHANISM, "OAUTHBEARER")
                .property(PROTOCOL, "SASL_PLAINTEXT")
                .property(OAUTH_TOKEN_URL, "http://keycloak/token")
                .property(OAUTH_REVOKE_URL, "http://keycloak/revoke")
                .property(OAUTH_CLIENT_ID, "client-id")
                .property(OAUTH_CLIENT_SECRET_KEY, "clientSecretKey")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenRequiredFieldsAreMissing() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("Kafka")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations)
                .satisfies(violations -> assertThat(violations).extracting(Violation::path)
                        .containsExactlyInAnyOrder(TOPIC, BOOTSTRAP_SERVERS, MECHANISM, PROTOCOL, OAUTH_TOKEN_URL,
                                OAUTH_REVOKE_URL, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET_KEY));
    }
}