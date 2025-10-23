package eu.dataspace.connector.tests;

import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
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

    static MdsParticipant inMemoryDcp(String name, IdentityHub identityHub, LazySupplier<String> issuerDid) {
        var did = identityHub.didFor(name).get();
        return MdsParticipant.Builder.newInstance()
                .id(did)
                .name(name)
                .runtime(participant -> baseRuntime(name, ":launchers:connector-inmemory-dcp", participant)
                        .configurationProvider(() -> {
                            identityHub.tokenEndpoint();
                            return ConfigFactory.fromMap(Map.ofEntries(
                                    Map.entry("edc.iam.sts.oauth.client.id", did),
                                    Map.entry("edc.iam.sts.oauth.client.secret.alias", did + "-sts-client-secret"),
                                    Map.entry("edc.iam.sts.oauth.token.url", identityHub.tokenEndpoint()),
                                    Map.entry("edc.iam.trusted-issuer.issuer.id", issuerDid.get()),
                                    Map.entry("edc.iam.trusted-issuer.issuer.supportedtypes", "[\"MembershipCredential\"]")
                            ));
                        })
                        .registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
                            @Inject
                            private Vault vault;

                            @Override
                            public void initialize(ServiceExtensionContext context) {
                                var participantContext = identityHub.participantContext(did);
                                // TODO: is there a way to get the alias as well?
                                vault.storeSecret(did + "-sts-client-secret", participantContext.clientSecret());
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
                        .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                                entry("edp.dataplane.callback.url", "http://localhost:8080"),
                                entry("edp.daseen.api.key", "api-key")))
                        )
                )
                .build();
    }

    static IdentityHub identityHub(String... participants) {
        var runtime = new EmbeddedRuntime("identity-hub", ":launchers:identity-hub");
        return new IdentityHub(runtime, participants);
    }

    static Issuer issuer() {
        var runtime = new EmbeddedRuntime("issuer", ":launchers:issuer");
        return new Issuer(runtime);
    }

    private static EmbeddedRuntime baseRuntime(String name, String module, MdsParticipant participant) {
        return new EmbeddedRuntime(name, module)
                .configurationProvider(participant::getConfiguration)
                .registerSystemExtension(ServiceExtension.class, participant.seedVaultKeys());
    }
}
