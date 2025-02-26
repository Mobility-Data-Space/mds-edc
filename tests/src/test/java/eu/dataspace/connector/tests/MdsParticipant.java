package eu.dataspace.connector.tests;

import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.security.AsymmetricKey;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static eu.dataspace.connector.tests.ConfigurationHelper.basicConfig;

public class MdsParticipant extends Participant {

    private MdsParticipant() {

    }

    public Config getConfiguration() {
        return basicConfig(id, controlPlaneManagement.get(), controlPlaneProtocol.get());
    }

    public ServiceExtension seedVaultKeys() {
        return new SeedVaultKeys();
    }

    public static class Builder extends Participant.Builder<MdsParticipant, Builder> {

        public static Builder newInstance() {
            return new Builder(new MdsParticipant());
        }

        protected Builder(MdsParticipant participant) {
            super(participant);
        }

    }

    private static class SeedVaultKeys implements ServiceExtension {

        @Inject
        private Vault vault;

        @Override
        public void initialize(ServiceExtensionContext context) {
            try {
                var kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                var keyPair = kpg.generateKeyPair();

                var privateKey = encode(keyPair.getPrivate());
                var publicKey = encode(keyPair.getPublic());

                vault.storeSecret("private-key-alias", privateKey);
                vault.storeSecret("public-key-alias", publicKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static @NotNull String encode(AsymmetricKey key) {
            var type = switch (key) {
                case PublicKey _ -> "PUBLIC";
                case PrivateKey _ -> "PRIVATE";
                default -> throw new EdcException("not possible");
            };

            return """
            -----BEGIN %s KEY-----
            %s
            -----END %s KEY-----
            """.formatted(type, Base64.getMimeEncoder().encodeToString(key.getEncoded()), type);
        }
    }
}
