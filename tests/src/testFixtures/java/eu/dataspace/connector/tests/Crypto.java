package eu.dataspace.connector.tests;


import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

public interface Crypto {

    static KeyPair generateKeyPair() {
        try {
            var kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static X509Certificate createCertificate(KeyPair keyPair) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            var dnName = new X500Name("CN=Test Certificate");
            var serialNumber = BigInteger.valueOf(System.currentTimeMillis());
            var startDate = Date.from(Instant.now());
            var endDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

            var certBuilder = new JcaX509v3CertificateBuilder(
                    dnName, serialNumber, startDate, endDate, dnName, keyPair.getPublic()
            );

            var contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
            return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certBuilder.build(contentSigner));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String encode(PublicKey key) {
        return encodeToString("PUBLIC KEY", key.getEncoded());
    }

    static String encode(PrivateKey key) {
        return encodeToString("PRIVATE KEY", key.getEncoded());
    }

    static String encode(X509Certificate certificate) {
        try {
            return encodeToString("CERTIFICATE", certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encodeToString(String type, byte[] bytes) {
        return """
            -----BEGIN %s-----
            %s
            -----END %s-----
            """.formatted(type, Base64.getMimeEncoder().encodeToString(bytes), type);
    }
}
