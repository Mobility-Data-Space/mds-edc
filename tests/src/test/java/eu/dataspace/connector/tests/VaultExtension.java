package eu.dataspace.connector.tests;

import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.vault.VaultContainer;

import java.util.UUID;

import static eu.dataspace.connector.tests.ConfigurationHelper.vaultConfig;

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
        return vaultConfig("http://localhost:" + vaultContainer.getFirstMappedPort(), token, name);
    }
}
