package eu.dataspace.connector.tests.extensions;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.vault.VaultContainer;

import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;

public class VaultExtension implements BeforeAllCallback, AfterAllCallback {

    private final String token = UUID.randomUUID().toString();
    private final VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.18.4").withVaultToken(token);

    @Override
    public void beforeAll(ExtensionContext context) {
        vaultContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        vaultContainer.stop();
    }

    public Config getConfig(String name) {
        var settings = Map.ofEntries(
                entry("edc.vault.hashicorp.url", "http://localhost:" + vaultContainer.getFirstMappedPort()),
                entry("edc.vault.hashicorp.token", token),
                entry("edc.vault.hashicorp.folder", name)
        );

        return ConfigFactory.fromMap(settings);
    }
}
