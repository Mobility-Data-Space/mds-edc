package eu.dataspace.connector.tests.extensions;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

public class ObserverServerExtension implements BeforeAllCallback, AfterAllCallback {

    private final LazySupplier<Integer> port = new LazySupplier<>(Ports::getFreePort);
    private final BlockingQueue<Event> events = new LinkedBlockingDeque<>();
    private WireMockServer server;
    private final String apiKey = UUID.randomUUID().toString();

    @Override
    public void beforeAll(ExtensionContext context) {
        server = new WireMockServer(WireMockConfiguration.options().port(port.get()));
        server.start();

        server.addMockServiceRequestListener((request, response) -> {
            var header = request.getHeader("X-Sender-ID");
            if (header == null) {
                throw new IllegalArgumentException("No X-Sender-ID header contained in the request");
            }
            events.add(new Event(header, request.getBodyAsString()));
        });

        server.stubFor(WireMock.any(WireMock.anyUrl())
            .withHeader("X-Api-Key", equalTo(apiKey))
            .willReturn(WireMock.aResponse().withStatus(202)));
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

    public String waitForEvent(String senderId, String eventType) {
        try {
            do {
                var event = events.poll(10, TimeUnit.SECONDS);
                if (event == null) {
                    throw new TimeoutException("No event of type " + eventType + " received");
                }

                if (event.senderId().equals(senderId) && event.body().contains(eventType)) {
                    return event.body();
                }
            } while (true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getBaseUrl() {
        return server.baseUrl();
    }

    record Event(String senderId, String body) {

    }
}
