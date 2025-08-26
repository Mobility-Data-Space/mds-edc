package eu.dataspace.connector.iam.daps;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

/**
 * Token decorator that sets the {@code scope} claim on the token that is used on DSP request egress
 */
public class DapsTokenDecorator implements TokenDecorator {
    private final String scope;

    public DapsTokenDecorator(String configuredScope) {
        this.scope = configuredScope;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParametersBuilder) {
        return tokenParametersBuilder.claims(JwtRegisteredClaimNames.SCOPE, scope);
    }
}
