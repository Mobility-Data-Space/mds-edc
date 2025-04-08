package eu.dataspace.connector.tests.feature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.junit.testfixtures.TestUtils.findBuildRoot;
import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

public class DockerImagesTest {

    @ParameterizedTest
    @ValueSource(strings = { "connector-inmemory", "connector-vault-postgresql", "connector-vault-postgresql-edp" })
    void shouldStartAndStopTheRuntime(String runtimeName) {
        var tarCreated = gradlewDistTar(runtimeName);

        assertThat(tarCreated).isTrue();

        var runtime = new GenericContainer<>(
                new ImageFromDockerfile().withDockerfile(findBuildRoot().toPath().resolve("launchers/Dockerfile"))
                        .withBuildArg("RUNTIME", runtimeName))
                .waitingFor(forLogMessage(".*Booting EDC runtime.*", 1))
                .withLogConsumer(f -> System.out.println(f.getUtf8StringWithoutLineEnding()));

        assertThatNoException().isThrownBy(() -> {
            runtime.start();

            runtime.stop();
        });
    }

    private boolean gradlewDistTar(String runtimeName) {
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
}
