package eu.dataspace.connector.tests.extensions;

import jakarta.json.Json;
import okhttp3.MultipartBody;
import okio.Buffer;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Map.entry;

public class LoggingHouseExtension implements BeforeAllCallback, AfterAllCallback {

    private final LazySupplier<Integer> port = new LazySupplier<>(Ports::getFreePort);
    private final BlockingQueue<String> events = new LinkedBlockingDeque<>();
    private WireMockServer server;

    @Override
    public void beforeAll(ExtensionContext context) {
        server = new WireMockServer(WireMockConfiguration.options().port(port.get()));
        server.start();

        var header = Json.createObjectBuilder()
                .add("@type", "ids:MessageProcessedNotificationMessage")
                .add("@id", UUID.randomUUID().toString())
                .build();

        var payload = Json.createObjectBuilder()
                .add("data", "any")
                .build();
        var body = new MultipartBody.Builder()
                .addFormDataPart("header", header.toString())
                .addFormDataPart("payload", payload.toString())
                .build();

        var buffer = serialize(body);
        
        server.stubFor(WireMock.any(WireMock.anyUrl())
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(buffer.readByteArray())
                .withHeader("Content-Type", body.contentType().type() + "/" + body.contentType().subtype() + "; boundary=" + body.boundary())));
        
        server.addMockServiceRequestListener((request, response) -> {
            events.add(request.getBodyAsString());
        });

    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (server != null) {
            server.stop();
        }
    }

    public Config getConfiguration() {
        return ConfigFactory.fromMap(Map.ofEntries(
                entry("edc.logginghouse.extension.enabled", "true"),
                entry("edc.logginghouse.extension.url", "http://localhost:" + port.get()),
                entry("edc.logginghouse.extension.workers.delay", "1"),
                entry("edc.logginghouse.extension.workers.period", "1")
        ));
    }

    private @NotNull Buffer serialize(MultipartBody body) {
        var buffer = new Buffer();
        try {
            body.writeTo(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buffer;
    }

    public String waitForEvent(String eventType) {
        try {
            do {
                var event = events.poll(10, TimeUnit.SECONDS);
                if (event == null) {
                    throw new TimeoutException("No event of type " + eventType + " received");
                }

                if (event.contains(eventType)) {
                    return event;
                }
            } while (true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
