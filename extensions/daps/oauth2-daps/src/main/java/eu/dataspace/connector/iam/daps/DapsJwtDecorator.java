package eu.dataspace.connector.iam.daps;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

public class DapsJwtDecorator implements TokenDecorator {

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        return tokenParameters.claims("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                .claims("@type", "ids:DatRequestToken");
    }
}
