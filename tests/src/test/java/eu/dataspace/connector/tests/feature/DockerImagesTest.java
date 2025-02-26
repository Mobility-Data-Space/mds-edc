package eu.dataspace.connector.tests.feature;

import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.stream.Stream;

import static eu.dataspace.connector.tests.ConfigurationHelper.basicConfig;
import static eu.dataspace.connector.tests.ConfigurationHelper.vaultConfig;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

public class DockerImagesTest {

    @ParameterizedTest
    @ArgumentsSource(Runtimes.class)
    void shouldStartAndStopTheRuntime(String runtimeName, Config config) {
        var tarCreated = gradlewDistTar(runtimeName);

        assertThat(tarCreated).isTrue();

        var runtime = new GenericContainer<>(
                new ImageFromDockerfile().withDockerfile(findBuildRoot().toPath().resolve("launchers/Dockerfile"))
                        .withBuildArg("RUNTIME", runtimeName))
                .withEnv(config.getRelativeEntries())
                .waitingFor(forLogMessage(".*ready.*", 1))
                .withLogConsumer(f -> System.out.println(f.getUtf8StringWithoutLineEnding()));

        assertThatNoException().isThrownBy(() -> {
            runtime.start();

            runtime.stop();
        });
    }

    private static class Runtimes implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments("connector-inmemory", basicConfig(UUID.randomUUID().toString(), randomURI(), randomURI())),
                    arguments("connector-vault", basicConfig(UUID.randomUUID().toString(), randomURI(), randomURI())
                            .merge(vaultConfig("http://localhost/any", UUID.randomUUID().toString(), UUID.randomUUID().toString())))
            );
        }

        private URI randomURI() {
            return URI.create("http://localhost:%d/%s".formatted(getFreePort(), UUID.randomUUID().toString()));
        }
    }

    private boolean gradlewDistTar(String runtimeName) {
        var buildRoot = findBuildRoot();
        var gradlew = new File(buildRoot, "gradlew");
        try {
            return Runtime.getRuntime()
                    .exec(new String[]{ gradlew.getCanonicalPath(), ":launchers:%s:distTar".formatted(runtimeName) })
                    .waitFor(1, MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File findBuildRoot() {
        try {
            return findBuildRoot(new File(".").getCanonicalFile());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private File findBuildRoot(File path) {
        var gradlew = new File(path, "gradlew");
        if (gradlew.exists()) {
            return path;
        }

        var parent = path.getParentFile();
        if (parent != null) {
            return findBuildRoot(parent);
        }
        return null;
    }
}
