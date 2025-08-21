package eu.dataspace.connector.iam.daps;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;

class DapsTokenDecoratorTest {

    @Test
    void decorate() {
        var decorator = new DapsTokenDecorator("test-scope");
        var bldr = TokenParameters.Builder.newInstance()
                .claims(AUDIENCE, "test-audience");

        var result = decorator.decorate(bldr).build();

        assertThat(result.getStringClaim(AUDIENCE)).isEqualTo("test-audience");
        assertThat(result.getStringClaim(SCOPE)).isEqualTo("test-scope");
    }

}
