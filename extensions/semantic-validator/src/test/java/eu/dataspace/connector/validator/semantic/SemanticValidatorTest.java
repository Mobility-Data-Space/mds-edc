package eu.dataspace.connector.validator.semantic;

import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class SemanticValidatorTest {

    private final JsonObjectValidator validator = SemanticValidator.instance();

    @Test
    void shouldSucceed() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder()))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenContainPropertyNotPartOfOntologyFile() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add("unrecognized", createArrayBuilder())))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1);
        });
    }

}
