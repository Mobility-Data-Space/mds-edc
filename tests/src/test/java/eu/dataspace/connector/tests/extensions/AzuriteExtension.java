package eu.dataspace.connector.tests.extensions;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class AzuriteExtension implements BeforeAllCallback, AfterAllCallback {

    private static final int port = 10_000;
    private static final String IMAGE_NAME = "mcr.microsoft.com/azure-storage/azurite:3.35.0";

    private final AzuriteContainer azuriteContainer;

    public AzuriteExtension(Account... accounts) {
        azuriteContainer = new AzuriteContainer(accounts);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        azuriteContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        azuriteContainer.stop();
    }

    public int getPort() {
        return azuriteContainer.getMappedPort(port);
    }

    public Config getConfig() {
        return ConfigFactory.fromMap(Map.of(
                "edc.blobstore.endpoint.template", endpointTemplate()
        ));
    }

    public BlobServiceClient getClient(Account account) {
        var client = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(account.name(), account.key()))
                .endpoint(endpointTemplate().formatted(account.name()))
                .buildClient();

        client.getAccountInfo();
        return client;
    }

    private @NotNull String endpointTemplate() {
        return "http://127.0.0.1:" + getPort() + "/%s";
    }

    public record Account(String name, String key) { }

    private static class AzuriteContainer extends GenericContainer<AzuriteContainer> {

        AzuriteContainer(Account... accounts) {
            super(IMAGE_NAME);
            addEnv("AZURITE_ACCOUNTS", stream(accounts)
                    .map(account -> "%s:%s".formatted(account.name(), account.key()))
                    .collect(joining(";")));
            addExposedPort(port);
            withLogConsumer(o -> System.out.println(o.getUtf8StringWithoutLineEnding()));
        }

    }
}
