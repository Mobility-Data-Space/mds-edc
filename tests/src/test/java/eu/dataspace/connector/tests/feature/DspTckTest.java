package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.Crypto;
import eu.dataspace.connector.tests.SeedVault;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.dsp.system.DspSystemLauncher;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspacetck.core.api.system.SystemsConstants.TCK_LAUNCHER;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@Tag("Tck")
public class DspTckTest {

    private static final URI PROTOCOL_URL = URI.create("http://localhost:" + getFreePort() + "/api/dsp");
    private static final URI WEBHOOK_URL = URI.create("http://localhost:" + getFreePort() + "/tck");

    @RegisterExtension
    static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
            .name("runtime")
            .modules(":launchers:connector-tck")
            .endpoints(Endpoints.Builder.newInstance()
                    .endpoint("protocol", () -> PROTOCOL_URL)
                    .endpoint("tck", () -> WEBHOOK_URL)
                    .endpoint("management", () -> URI.create("http://localhost:%d/api/management".formatted(getFreePort())))
                    .build())
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.participant.id", "participantContextId",
                    "web.api.auth.key", "password",
                    "edc.component.id", "DSP-compatibility-test",
                    "edc.transfer.proxy.token.verifier.publickey.alias", "public-key-alias",
                    "edc.transfer.proxy.token.signer.privatekey.alias", "private-key-alias"
            )))
            .build()
            .registerSystemExtension(ServiceExtension.class, DspTckTest.seedVaultKeys());

    private static ServiceExtension seedVaultKeys() {
        var keyPair = Crypto.generateKeyPair();
        var map = Map.of(
                "private-key-alias", Crypto.encode(keyPair.getPrivate()),
                "public-key-alias", Crypto.encode(keyPair.getPublic())
        );
        return SeedVault.fromMap(c -> map);
    }

    @Timeout(300)
    @Test
    void assertDspCompatibility() throws IOException {
        var monitor = new ConsoleMonitor(true, true);

        var result = TckRuntime.Builder.newInstance()
                .properties(loadProperties())
                .property(TCK_LAUNCHER, DspSystemLauncher.class.getName())
                .property("dataspacetck.debug", "true")
                .addPackage("org.eclipse.dataspacetck.dsp.verification")
                .monitor(monitor)
                .build()
                .execute();

        assertThat(result.getTestsStartedCount()).isGreaterThan(0);

        var failures = result.getFailures().stream().map(this::mapFailure).toList();

        assertThat(failures).isEmpty();
    }

    private Map<String, String> loadProperties() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(resourceConfig("tck/tck.properties"))) {
            properties.load(reader);
        }

        properties.put("dataspacetck.dsp.connector.http.url", PROTOCOL_URL + "/2025-1");
        properties.put("dataspacetck.dsp.connector.http.base.url", PROTOCOL_URL);
        properties.put("dataspacetck.dsp.connector.negotiation.initiate.url", WEBHOOK_URL + "/negotiations/requests");
        properties.put("dataspacetck.dsp.connector.transfer.initiate.url", WEBHOOK_URL + "/transfers/requests");
        properties.put("dataspacetck.dsp.connector.agent.id", "participantContextId");
        properties.put("dataspacetck.dsp.connector.http.headers.authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}");
        properties.put("dataspacetck.dsp.jsonld.context.edc.uri", "https://w3id.org/edc/dspace/v0.0.1");
        properties.put("dataspacetck.dsp.jsonld.context.edc.path", resourceConfig("tck/dspace-edc-context-v1.jsonld"));

        return properties.entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    private String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }

    private TestResult mapFailure(TestExecutionSummary.Failure failure) {
        var displayName = failure.getTestIdentifier().getDisplayName().split(":");
        return new TestResult(format("%s:%s", displayName[0], displayName[1]), failure);
    }

    private record TestResult(String testId, TestExecutionSummary.Failure failure) {

        @Override
        public @NotNull String toString() {
            return "- " + failure.getTestIdentifier().getDisplayName() + " (" + failure.getException() + ")";
        }
    }

}
