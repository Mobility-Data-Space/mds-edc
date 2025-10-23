package eu.dataspace.connector.tests;

import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.entry;

public interface MdsParticipantFactory {

    static MdsParticipant inMemory(String name) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .runtime(participant -> baseRuntime(name, ":launchers:connector-inmemory", participant))
                .build();
    }

    static MdsParticipant inMemoryDcp(String name, Wallet wallet, LazySupplier<String> issuerDid) {
        var did = wallet.didFor(name).get();
        var stsClientSecretAlias = did + "-sts-client-secret";
        return MdsParticipant.Builder.newInstance()
                .id(did)
                .name(name)
                .runtime(participant -> baseRuntime(name, ":launchers:connector-inmemory-dcp", participant)
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                                Map.entry("edc.iam.sts.oauth.client.id", did),
                                Map.entry("edc.iam.sts.oauth.client.secret.alias", stsClientSecretAlias),
                                Map.entry("edc.iam.sts.oauth.token.url", wallet.tokenEndpoint()),
                                Map.entry("edc.iam.trusted-issuer.issuer.id", issuerDid.get()),
                                Map.entry("edc.iam.trusted-issuer.issuer.supportedtypes", "[\"MembershipCredential\"]")
                        )))
                        .registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
                            @Inject
                            private Vault vault;

                            @Override
                            public void initialize(ServiceExtensionContext context) {
                                var participantContext = wallet.participantContext(did);
                                vault.storeSecret(stsClientSecretAlias, participantContext.clientSecret());
                            }
                        })
                )
                .build();
    }

    static MdsParticipant tck(String name) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .eventReceiver(false)
                .runtime(participant -> baseRuntime(name, ":launchers:connector-tck", participant))
                .build();
    }

    static MdsParticipant hashicorpVault(String name, VaultExtension vault, SovityDapsExtension daps, PostgresqlExtension postgres) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .runtime(participant -> baseRuntime(name, ":launchers:connector-vault-postgresql", participant)
                        .configurationProvider(() -> vault.getConfig(name))
                        .configurationProvider(() -> daps.dapsConfig(name))
                        .registerSystemExtension(ServiceExtension.class, daps.seedDapsKeyPair())
                        .configurationProvider(() -> postgres.getConfig(name))
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                                "org.eclipse.tractusx.edc.postgresql.migration.schema", postgres.getSchema()
                        )))
                )
                .build();
    }

    static MdsParticipant edp(String name, VaultExtension vault, SovityDapsExtension daps, PostgresqlExtension postgres) {
        return MdsParticipant.Builder.newInstance()
                .id(name)
                .name(name)
                .runtime(participant -> baseRuntime(name, ":launchers:connector-vault-postgresql-edp", participant)
                        .configurationProvider(() -> vault.getConfig(name))
                        .configurationProvider(() -> daps.dapsConfig(name))
                        .registerSystemExtension(ServiceExtension.class, daps.seedDapsKeyPair())
                        .configurationProvider(() -> postgres.getConfig(name))
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                                "org.eclipse.tractusx.edc.postgresql.migration.schema", postgres.getSchema()
                        )))
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                                entry("edp.dataplane.callback.url", "http://localhost:8080"),
                                entry("edp.daseen.api.key", "api-key")))
                        )
                )
                .build();
    }

    static Wallet wallet(PostgresqlExtension postgres, VaultExtension vault, String... participants) {
        var name = "wallet";
        var runtime = new EmbeddedRuntime(name, ":launchers:wallet")
                .configurationProvider(() -> vault.getConfig(name))
                .configurationProvider(() -> postgres.getConfig(name))
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        "eu.dataspace.wallet.postgresql.migration.schema", postgres.getSchema()
                )));
        return new Wallet(runtime, participants);
    }

    static Issuer issuer(PostgresqlExtension postgres, VaultExtension vault) {
        var name = "issuer";
        var runtime = new EmbeddedRuntime(name, ":launchers:issuer")
                .configurationProvider(() -> vault.getConfig(name))
                .configurationProvider(() -> postgres.getConfig(name))
                .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        "eu.dataspace.issuer.postgresql.migration.schema", postgres.getSchema()
                )));
        return new Issuer(runtime);
    }

    private static EmbeddedRuntime baseRuntime(String name, String module, MdsParticipant participant) {
        return new EmbeddedRuntime(name, module)
                .configurationProvider(participant::getConfiguration)
                .registerSystemExtension(ServiceExtension.class, participant.seedVaultKeys());
    }
}
