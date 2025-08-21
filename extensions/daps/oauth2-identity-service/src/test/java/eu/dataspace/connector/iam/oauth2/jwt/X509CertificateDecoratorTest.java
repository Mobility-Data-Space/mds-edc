package eu.dataspace.connector.iam.oauth2.jwt;

import eu.dataspace.connector.tests.Crypto;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class X509CertificateDecoratorTest {

    @Test
    void verifyDecorator() {
        var certificate = Crypto.createCertificate(Crypto.generateKeyPair());;
        var decorator = new X509CertificateDecorator(certificate);

        var builder = TokenParameters.Builder.newInstance();
        decorator.decorate(builder);

        var tokenParams = builder.build();
        assertThat(tokenParams.getClaims()).isEmpty();
        assertThat(tokenParams.getHeaders()).containsOnlyKeys("x5t");
    }
}
