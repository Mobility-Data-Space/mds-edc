package eu.dataspace.connector.tests;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.Map;

import static java.util.Map.entry;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public interface ConfigurationHelper {

    static Config basicConfig(String id, URI managementApi, URI protocolApi) {
        var settings = Map.ofEntries(
                entry("edc.participant.id", id),
                entry("web.http.path", "/api"),
                entry("web.http.port", getFreePort() + ""),
                entry("web.http.control.path", "/control"),
                entry("web.http.control.port", getFreePort() + ""),
                entry("web.http.management.path", managementApi.getPath()),
                entry("web.http.management.port", managementApi.getPort() + ""),
                entry("web.http.protocol.path", protocolApi.getPath()),
                entry("web.http.protocol.port", protocolApi.getPort() + ""),
                entry("web.http.version.path", "/version"),
                entry("web.http.version.port", getFreePort() + ""),
                entry("web.http.public.path", "/public"),
                entry("web.http.public.port", getFreePort() + ""),
                entry("edc.transfer.proxy.token.verifier.publickey.alias", "public-key-alias"),
                entry("edc.transfer.proxy.token.signer.privatekey.alias", "private-key-alias")
        );

        return ConfigFactory.fromMap(settings);
    }

    static Config vaultConfig(String url, String token, String folder) {
        var settings = Map.ofEntries(
                entry("edc.vault.hashicorp.url", url),
                entry("edc.vault.hashicorp.token", token),
                entry("edc.vault.hashicorp.folder", folder)
        );

        return ConfigFactory.fromMap(settings);
    }
}
