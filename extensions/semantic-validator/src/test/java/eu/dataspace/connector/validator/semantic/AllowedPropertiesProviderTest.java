package eu.dataspace.connector.validator.semantic;

import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AllowedPropertiesProviderTest {

    private final JsonLd jsonLd = new TitaniumJsonLd(mock());

    @Test
    void shouldExtractPropertyKeysFromExampleFile() {
        var provider = new AllowedPropertiesProvider(jsonLd);

        var allowedKeys = provider.provide();

        assertThat(allowedKeys).isNotEmpty().containsAnyOf("http://purl.org/dc/terms/title");
    }
}
