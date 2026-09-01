package eu.dataspace.connector.tests.extensions;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.util.io.Ports;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.awaitility.Awaitility.await;

public class ObserverServerExtension implements BeforeAllCallback, AfterAllCallback {

    private final LazySupplier<Integer> port = new LazySupplier<>(Ports::getFreePort);
    private WireMockServer server;
    private final String apiKey = UUID.randomUUID().toString();

    @Override
    public void beforeAll(ExtensionContext context) {
        server = new WireMockServer(WireMockConfiguration.options().port(port.get()));
        server.start();
        stubUp();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (server != null) {
            server.stop();
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public List<String> receivedEvents() {
        return allEvents()
                .map(e -> e.getRequest().getBodyAsString())
                .toList();
    }

    public void clearEvents() {
        server.resetRequests();
    }

    public String waitForEvent(String senderId, String eventType) {
        return await().atMost(10, TimeUnit.SECONDS)
                .until(() -> allEvents()
                        .filter(e -> senderId.equals(e.getRequest().getHeader("X-Sender-ID")))
                        .filter(e -> e.getRequest().getBodyAsString().contains(eventType))
                        .findFirst(), Optional::isPresent)
                .get().getRequest().getBodyAsString();
    }

    public void simulateDown() {
        server.resetMappings();
        server.stubFor(WireMock.any(WireMock.anyUrl())
                .willReturn(WireMock.aResponse().withStatus(503)));
    }

    public void simulateUp() {
        server.resetMappings();
        stubUp();
    }

    private @NonNull Stream<ServeEvent> allEvents() {
        return server.getAllServeEvents().stream()
                .filter(e -> e.getResponse().getStatus() / 100 == 2);
    }

    private void stubUp() {
        server.stubFor(WireMock.any(WireMock.anyUrl())
                .withHeader("X-Api-Key", equalTo(apiKey))
                .willReturn(WireMock.aResponse().withStatus(202)));
    }

    public String getBaseUrl() {
        return server.baseUrl();
    }
}
