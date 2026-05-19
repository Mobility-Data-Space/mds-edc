package eu.dataspace.connector.iam.oauth2.certificate;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class VaultCertificateResolver {
    public static final String HEADER = "-----BEGIN CERTIFICATE-----";
    public static final String FOOTER = "-----END CERTIFICATE-----";
    public static final String EDC_EXCEPTION_MESSAGE = "Found certificate with id [%s], but failed to convert it";

    @NotNull
    private final Vault vault;

    public VaultCertificateResolver(@NotNull Vault vault) {
        this.vault = vault;
    }

    public @Nullable X509Certificate resolveCertificate(String id) {
        var certificateRepresentation = vault.resolveSecret(id);
        if (certificateRepresentation == null) {
            return null;
        }

        try {
            var encoded = certificateRepresentation.replace(HEADER, "").replaceAll("\\R", "").replace(FOOTER, "");
            var fact = CertificateFactory.getInstance("X.509");
            return (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(encoded.getBytes())));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new EdcException(String.format(EDC_EXCEPTION_MESSAGE, id), e);
        }
    }
}
