package eu.dataspace.connector.iam.oauth2.certificate;

import eu.dataspace.connector.tests.Crypto;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaultCertificateResolverTest {
    private static final String KEY = "key";

    private final Vault vault = mock(Vault.class);
    private final VaultCertificateResolver certificateResolver = new VaultCertificateResolver(vault);

    @Test
    void resolveCertificate() throws RuntimeException {
        var encoded = Crypto.encode(Crypto.createCertificate(Crypto.generateKeyPair()));
        when(vault.resolveSecret(KEY)).thenReturn(encoded);

        var certificate = Objects.requireNonNull(certificateResolver.resolveCertificate(KEY));
        var pemReceived = Crypto.encode(certificate);

        verify(vault, times(1)).resolveSecret(KEY);
        assertThat(pemReceived.split("\\R")).isEqualTo(encoded.split("\\R"));
    }

    @Test
    void resolveCertificate_notFound() {
        when(vault.resolveSecret(KEY)).thenReturn(null);

        var certificate = certificateResolver.resolveCertificate(KEY);

        verify(vault, times(1)).resolveSecret(KEY);
        assertThat(certificate).isNull();
    }

    @Test
    void resolveCertificate_conversionError() {
        when(vault.resolveSecret(KEY)).thenReturn("Not a PEM");

        Exception exception = assertThrows(EdcException.class, () -> certificateResolver.resolveCertificate(KEY));

        assertThat(exception.getMessage()).isEqualTo(String.format(VaultCertificateResolver.EDC_EXCEPTION_MESSAGE, KEY));
    }

}