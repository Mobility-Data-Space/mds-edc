package org.eclipse.edc.compatibility.tests.fixtures;

import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class MdsCeConnector extends GenericContainer<MdsCeConnector> {

    public MdsCeConnector(String version, String name, Map<String, String> env) {
        super("ghcr.io/sovity/edc-ce-mds:" + version);
        this.withCreateContainerCmdModifier(cmd -> cmd.withName(name));
        this.withNetworkMode("host");
        this.waitingFor(Wait.forLogMessage(".*Runtime .* ready.*", 1));
        this.withEnv(env);
        this.withLogConsumer(it -> System.out.print(name + ":" + it.getUtf8String()));
    }

    public MdsCeConnector copyResource(String resourceName) {
        return copyResource(resourceName, resourceName);
    }

    public MdsCeConnector copyResource(String resourceName, String containerPath) {
        try {
            var bytes = Files.readAllBytes(TestUtils.getFileFromResourceName(resourceName).toPath());
            return withCopyToContainer(Transferable.of(bytes), "/app/" + containerPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
