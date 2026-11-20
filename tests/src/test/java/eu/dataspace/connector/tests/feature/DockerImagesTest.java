package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.junit.testfixtures.TestUtils.findBuildRoot;

/**
 * Boots the published Docker images with the configuration of the embedded tests, and verifies that they bundle the
 * extensions whose absence would go unnoticed, because EDC would silently fall back to a default provider
 * (e.g. InMemoryVault, in-memory stores).
 */
class DockerImagesTest {

    private static final Pattern INITIALIZED = Pattern.compile("^DEBUG \\S+ Initialized (.+)$");

    private static final List<String> VAULT_EXTENSIONS = List.of(
            // libs.edc.vault.hashicorp
            "Hashicorp Vault"
    );
    private static final List<String> STORE_EXTENSIONS = List.of(
            // libs.edc.controlplane.feature.sql.bom
            "org.eclipse.edc.connector.controlplane.store.sql.assetindex.SqlAssetIndexServiceExtension",
            // :extensions:data-plane:sql
            "Sql Data Plane Store",
            "SQL Data Source Store",
            "Sql AccessTokenData Store",
            "SQL Observer Event Store"
    );
    private static final List<String> WALLET_EXTENSIONS = List.of(
            // libs.edc.identityhub.feature.sql.bom
            "org.eclipse.edc.identityhub.store.sql.credentials.SqlCredentialStoreExtension"
    );

    @Nested
    class InMemory {

        private static final MdsParticipant CONNECTOR = participant("connector");

        @Test
        void shouldBoot() {
            try (var runtime = launcher("connector-inmemory", CONNECTOR.getConfiguration())) {
                assertThatNoException().isThrownBy(runtime::start);
            }
        }

        @Test
        void shouldBootDcp() {
            try (var runtime = launcher("connector-inmemory-dcp", CONNECTOR.getConfiguration())) {
                assertThatNoException().isThrownBy(runtime::start);
            }
        }
    }

    @Nested
    class Daps {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES = new PostgresqlExtension("provider");

        @RegisterExtension
        @Order(2)
        private static final SovityDapsExtension DAPS = new SovityDapsExtension();

        private static final MdsParticipant CONNECTOR = participant("provider");

        @Test
        void mustInitializeConnectorExtensions() {
            var name = CONNECTOR.getName();
            DAPS.dapsKeyPair(name).forEach((key, value) -> VAULT.storeSecret(name, key, value));
            var config = CONNECTOR.getConfiguration()
                    .merge(VAULT.getConfig(name))
                    .merge(POSTGRES.getConfig(name))
                    .merge(DAPS.dapsConfig(name));

            try (var runtime = launcher("connector-vault-postgresql", config)) {
                runtime.start();

                var extensions = initializedExtensions(runtime);
                assertThat(extensions).containsAll(VAULT_EXTENSIONS);
                assertThat(extensions).containsAll(STORE_EXTENSIONS);
            }
        }
    }

    @Nested
    class Dcp {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES = new PostgresqlExtension("wallet", "provider");

        private static final MdsParticipant CONNECTOR = participant("provider");

        // not registered as extension: only its configuration is needed, its embedded runtime never boots
        private static final Wallet WALLET = new Wallet(new EmbeddedRuntime("wallet", new URL[0]), new String[0]);

        @Test
        void mustInitializeConnectorExtensions() {
            var name = CONNECTOR.getName();
            var config = CONNECTOR.getConfiguration()
                    .merge(VAULT.getConfig(name))
                    .merge(POSTGRES.getConfig(name));

            try (var runtime = launcher("connector-vault-postgresql-dcp", config)) {
                runtime.start();

                var extensions = initializedExtensions(runtime);
                assertThat(extensions).containsAll(VAULT_EXTENSIONS);
                assertThat(extensions).containsAll(STORE_EXTENSIONS);
            }
        }

        @Test
        void mustInitializeWalletExtensions() {
            var name = "wallet";
            var config = WALLET.configuration()
                    .merge(MdsParticipantFactory.walletConfiguration(POSTGRES))
                    .merge(VAULT.getConfig(name))
                    .merge(POSTGRES.getConfig(name));

            try (var runtime = launcher("wallet", config)) {
                runtime.start();

                var extensions = initializedExtensions(runtime);
                assertThat(extensions).containsAll(VAULT_EXTENSIONS);
                assertThat(extensions).containsAll(WALLET_EXTENSIONS);
            }
        }
    }

    private static MdsParticipant participant(String name) {
        return MdsParticipant.Builder.newInstance().id(name).name(name).build();
    }

    /**
     * The launcher's image, configured like its embedded runtime: the host network makes the configuration
     * (localhost endpoints, ports) work unchanged.
     */
    private static GenericContainer<?> launcher(String launcher, Config config) {
        assertThat(gradlewDistTar(launcher)).isTrue();

        var image = new ImageFromDockerfile()
                .withDockerfile(findBuildRoot().toPath().resolve("launchers/Dockerfile"))
                .withBuildArg("RUNTIME", launcher);

        return new GenericContainer<>(image)
                .withNetworkMode("host")
                .withEnv(config.getEntries())
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("/app/bin/" + launcher, "--log-level=DEBUG", "--no-color"))
                .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                .waitingFor(Wait.forLogMessage("(?s).*Runtime .* ready.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
    }

    private static boolean gradlewDistTar(String runtimeName) {
        var buildRoot = findBuildRoot();
        var gradlew = new File(buildRoot, "gradlew");
        try {
            return Runtime.getRuntime()
                    .exec(new String[]{gradlew.getCanonicalPath(), ":launchers:%s:distTar".formatted(runtimeName)})
                    .waitFor(1, MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Names of the extensions that got initialized, as logged at DEBUG level by the EDC boot sequence.
     */
    private static Set<String> initializedExtensions(GenericContainer<?> runtime) {
        return runtime.getLogs().lines()
                .map(INITIALIZED::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1).trim())
                .collect(toSet());
    }
}
