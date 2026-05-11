package eu.dataspace.connector.tests.feature;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.IssuerExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.S3Extension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static jakarta.json.Json.createObjectBuilder;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class S3DataSourceTest {

    @Nested
    class Daps extends Tests {

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
        private static final S3Extension PROVIDER_S3 = new S3Extension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

        protected Daps() {
            super(PROVIDER, CONSUMER, PROVIDER_S3);
        }
    }

    @Nested
    class Dcp extends Tests {

        @RegisterExtension
        @Order(0)
        private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

        @RegisterExtension
        @Order(0)
        private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("issuer", "wallet", "consumer", "provider");

        @RegisterExtension
        @Order(1)
        private static final IssuerExtension ISSUER = new IssuerExtension(POSTGRES_EXTENSION, VAULT_EXTENSION);

        @RegisterExtension
        @Order(2)
        private static final Wallet IDENTITY_HUB = MdsParticipantFactory.wallet(POSTGRES_EXTENSION, VAULT_EXTENSION, "consumer", "provider");

        @RegisterExtension
        @Order(3)
        private static final S3Extension PROVIDER_S3 = new S3Extension();

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVaultDcp("provider", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        @RegisterExtension
        @Order(4)
        private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVaultDcp("consumer", VAULT_EXTENSION, POSTGRES_EXTENSION, IDENTITY_HUB, ISSUER.did());

        protected Dcp() {
            super(PROVIDER, CONSUMER, PROVIDER_S3);
        }

        @BeforeAll
        static void setUp() {
            ISSUER.registerAttestationAndCredentialDefinition();
            ISSUER.registerHolder(PROVIDER.getId(), PROVIDER.getName());
            ISSUER.registerHolder(CONSUMER.getId(), CONSUMER.getName());
            IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
            IDENTITY_HUB.requestCredentialIssuance(CONSUMER.getId(), ISSUER.did().get());
        }
    }

    abstract static class Tests {

        private final MdsParticipant provider;
        private final MdsParticipant consumer;
        private final S3Extension providerS3;

        protected Tests(MdsParticipant provider, MdsParticipant consumer, S3Extension providerS3) {
            this.provider = provider;
            this.consumer = consumer;
            this.providerS3 = providerS3;
        }

        @Test
        void shouldSupportS3DataSourceTransfer() {
            var consumerDataDestination = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
            consumerDataDestination.start();
            consumerDataDestination.stubFor(post(urlPathEqualTo("/destination")).willReturn(ok()));

            var fileContent = UUID.randomUUID().toString().getBytes();
            var bucketName = UUID.randomUUID().toString();
            var objectName = UUID.randomUUID().toString();

            var userAccessKey = providerS3.createBucket(bucketName);
            providerS3.uploadToBucket(bucketName, objectName, fileContent);

            provider.getService(Vault.class).storeSecret("s3credentials", Json.createObjectBuilder()
                    .add("edctype", "dataspaceconnector:secrettoken")
                    .add("accessKeyId", userAccessKey.accessKeyId())
                    .add("secretAccessKey", userAccessKey.secretAccessKey())
                    .build().toString());

            Map<String, Object> dataAddressProperties = Map.of(
                    EDC_NAMESPACE + "type", "AmazonS3",
                    EDC_NAMESPACE + "keyName", "s3credentials",
                    EDC_NAMESPACE + "endpointOverride", providerS3.getEndpoint().toString(),
                    EDC_NAMESPACE + "region", "eu-central-1",
                    EDC_NAMESPACE + "bucketName", bucketName,
                    EDC_NAMESPACE + "objectName", objectName
            );

            var assetId = provider.createOffer(dataAddressProperties);

            var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PUSH")
                    .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.port() + "/destination"))
                    .execute();

            consumer.awaitTransferToBeInState(transferProcessId, COMPLETED);

            await().untilAsserted(() -> {
                consumerDataDestination.verify(anyRequestedFor(urlPathEqualTo("/destination"))
                        .withRequestBody(binaryEqualTo(fileContent)));
            });

            consumerDataDestination.stop();
        }

        private JsonObject httpDataAddress(String baseUrl) {
            return createObjectBuilder()
                    .add(TYPE, EDC_NAMESPACE + "DataAddress")
                    .add(EDC_NAMESPACE + "type", "HttpData")
                    .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                    .build();
        }
    }
}
