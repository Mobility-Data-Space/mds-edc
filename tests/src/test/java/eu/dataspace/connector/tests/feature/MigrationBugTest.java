package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Map;

import static eu.dataspace.connector.tests.Crypto.createCertificate;
import static eu.dataspace.connector.tests.Crypto.encode;
import static eu.dataspace.connector.tests.Crypto.generateKeyPair;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * This test covers this case: https://github.com/Mobility-Data-Space/mds-edc/issues/338
 *
 * It can be removed as soon as we reach version 1.0.0.
 */
@Deprecated(since = "1.0.0-rc.12")
class MigrationBugTest {

    @RegisterExtension
    @Order(0)
    private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

    @RegisterExtension
    @Order(1)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider");

    @RegisterExtension
    @Order(2)
    private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

    @Test
    void shouldNotThrowExceptions_whenMigratingFromRc10ToLatest() {
        runMigrations(getGenericContainer());

        assertThatNoException().isThrownBy(() -> runMigrations(prepareConnectorAtLatestVersion()));
    }

    private GenericContainer<? extends GenericContainer<?>> getGenericContainer() {
        return prepareConnectorAtVersion("1.0.0-rc.10");
    }

    private void runMigrations(MdsParticipant provider) {
        provider.getRuntime().boot();
    }

    private MdsParticipant prepareConnectorAtLatestVersion() {
        return MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);
    }

    private void runMigrations(GenericContainer<? extends GenericContainer<?>> selfGenericContainer) {
        selfGenericContainer.start();
        selfGenericContainer.stop();
    }

    private GenericContainer<?> prepareConnectorAtVersion(String version) {
        var keyPair = generateKeyPair();
        VAULT_EXTENSION.storeSecret("provider", "daps-private-key", encode(keyPair.getPrivate()));
        VAULT_EXTENSION.storeSecret("provider", "daps-certificate", encode(createCertificate(keyPair)));
        POSTGRES_EXTENSION.getConfig("provider").getEntries();
        return new GenericContainer<>("ghcr.io/mobility-data-space/mds-edc/connector-vault-postgresql:" + version)
                .withLogConsumer(o -> System.out.println(o.getUtf8StringWithoutLineEnding()))
                .withNetworkMode("host")
                .withEnv(Map.ofEntries(
                        entry("edc.participant.id", "provider"),
                        entry("edc.participant.context.id", "provider"),
                        entry("web.http.management.port", String.valueOf(getFreePort())),
                        entry("edc.transfer.proxy.token.signer.privatekey.alias", "any"),
                        entry("edc.transfer.proxy.token.verifier.publickey.alias", "any"),
                        entry("edc.logginghouse.extension.enabled", "false")
                ))
                .withEnv(VAULT_EXTENSION.getConfig("provider").getEntries())
                .withEnv(DAPS_EXTENSION.dapsConfig("provider").getEntries())
                .withEnv(POSTGRES_EXTENSION.getConfig("provider").getEntries())
                .waitingFor(Wait.forLogMessage(".*Runtime.*ready.*", 1));
    }

}
