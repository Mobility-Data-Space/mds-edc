package eu.dataspace.connector.extension.validator.dataaddress.kafka;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class KafkaBrokerDataAddressValidatorExtensionTest {

    private final DataAddressValidatorRegistry dataAddressValidatorRegistry = mock();

    @BeforeEach
    void setUp(final ServiceExtensionContext context) {
        context.registerService(DataAddressValidatorRegistry.class, dataAddressValidatorRegistry);
    }

    @Test
    void initialize_shouldRegisterValidatorsWithKafkaType(final KafkaBrokerDataAddressValidatorExtension extension, final ServiceExtensionContext context) {
        // Arrange
        extension.initialize(context);

        verify(dataAddressValidatorRegistry, times(1))
                .registerSourceValidator(anyString(), any(KafkaBrokerDataAddressValidator.class));
        verify(dataAddressValidatorRegistry, times(1))
                .registerDestinationValidator(anyString(), any(KafkaBrokerDataAddressValidator.class));
    }
}