package eu.dataspace.connector.tests;

import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.entry;

public interface MdsParticipantFactory {

    static MdsParticipant inMemory(String name) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .runtime(participant -> new EmbeddedRuntime(name, ":launchers:connector-inmemory")
                        .configurationProvider(participant::getConfiguration)
                        .registerSystemExtension(ServiceExtension.class, participant.seedVaultKeys()))
                .build();
    }

    static MdsParticipant hashicorpVault(String name, VaultExtension vault, SovityDapsExtension daps, PostgresqlExtension postgres) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .runtime(participant -> new EmbeddedRuntime(name, ":launchers:connector-vault-postgresql")
                        .configurationProvider(participant::getConfiguration)
                        .configurationProvider(() -> vault.getConfig(name))
                        .registerSystemExtension(ServiceExtension.class, participant.seedVaultKeys())
                        .configurationProvider(() -> daps.dapsConfig(name))
                        .registerSystemExtension(ServiceExtension.class, daps.seedDapsKeyPair())
                        .configurationProvider(() -> postgres.getConfig(name))
                )
                .build();
    }

    static MdsParticipant edp(String name, VaultExtension vault, SovityDapsExtension daps, PostgresqlExtension postgres) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .runtime(participant -> new EmbeddedRuntime(name, ":launchers:connector-vault-postgresql-edp")
                        .configurationProvider(participant::getConfiguration)
                        .configurationProvider(() -> vault.getConfig(name))
                        .registerSystemExtension(ServiceExtension.class, participant.seedVaultKeys())
                        .configurationProvider(() -> daps.dapsConfig(name))
                        .registerSystemExtension(ServiceExtension.class, daps.seedDapsKeyPair())
                        .configurationProvider(() -> postgres.getConfig(name))
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                                entry("edp.dataplane.callback.url", "http://localhost:8080"),
                                entry("edp.daseen.api.key", "api-key")))
                        )
                )
                .build();
    }
}
