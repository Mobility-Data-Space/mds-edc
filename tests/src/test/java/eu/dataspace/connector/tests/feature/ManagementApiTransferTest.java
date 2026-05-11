package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.atomicConstraint;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.policy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

class ManagementApiTransferTest {

    @Nested
    class InMemory extends AbstractTransferTests {

        @RegisterExtension
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.inMemory("provider");

        @RegisterExtension
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.inMemory("consumer");

        protected InMemory() {
            super(PROVIDER, CONSUMER);
        }
    }

    @Nested
    class HashicorpVaultPostgresql extends AbstractTransferTests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(1)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer");

        @RegisterExtension
        @Order(2)
        private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        @Order(3)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected HashicorpVaultPostgresql() {
            super(PROVIDER, CONSUMER);
        }

        @Test
        void shouldNotOfferAssets_whenPolicyIsConstrained() {
            var assetId = UUID.randomUUID().toString();
            PROVIDER.createAsset(assetId, Collections.emptyMap(), Map.of("type", "HttpData", "baseUrl", "http://any"));
            var permission = createObjectBuilder()
                    .add("action", "use")
                    .add("constraint", atomicConstraint("REFERRING_CONNECTOR", "eq", "not-consumer"))
                    .build();
            var policyId = PROVIDER.createPolicyDefinition(policy(List.of(permission)));
            PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);

            var catalogDatasets = CONSUMER.getCatalogDatasets(PROVIDER);

            assertThat(catalogDatasets).noneSatisfy(dataset ->
                    assertThat(dataset.asJsonObject().getString(ID)).isEqualTo(assetId)
            );
        }
    }

}
