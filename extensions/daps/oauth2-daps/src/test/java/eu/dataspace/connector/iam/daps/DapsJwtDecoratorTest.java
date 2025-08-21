package eu.dataspace.connector.iam.daps;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DapsJwtDecoratorTest {

    private final DapsJwtDecorator decorator = new DapsJwtDecorator();

    @Test
    void verifyDecorate() {

        var builder = TokenParameters.Builder.newInstance();
        decorator.decorate(builder);

        var tokenParams = builder.build();
        assertThat(tokenParams.getHeaders()).isEmpty();
        assertThat(tokenParams.getClaims())
                .hasFieldOrPropertyWithValue("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .hasFieldOrPropertyWithValue("@type", "ids:DatRequestToken");
    }

}
