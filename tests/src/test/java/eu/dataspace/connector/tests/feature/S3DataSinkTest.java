package eu.dataspace.connector.tests.feature;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.S3Extension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class S3DataSinkTest {

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
    private static final S3Extension CONSUMER_S3 = new S3Extension();

    @RegisterExtension
    @Order(4)
    private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

    @RegisterExtension
    @Order(4)
    private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION);

    @Test
    void shouldSupportS3DataSinkTransfer() {
        var providerDataSource = new WireMockServer(WireMockConfiguration.options().port(getFreePort()));
        providerDataSource.start();
        providerDataSource.stubFor(WireMock.any(WireMock.urlEqualTo("/source"))
            .willReturn(WireMock.aResponse().withBody("data")));

        var bucketName = UUID.randomUUID().toString();

        var userAccessKey = CONSUMER_S3.createBucket(bucketName);

        CONSUMER.getService(Vault.class).storeSecret("s3credentials", Json.createObjectBuilder()
                .add("edctype", "dataspaceconnector:secrettoken")
                .add("accessKeyId", userAccessKey.accessKeyId())
                .add("secretAccessKey", userAccessKey.secretAccessKey())
                .build().toString());

        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "baseUrl", "http://localhost:%s/source".formatted(providerDataSource.port())
        );

        var assetId = PROVIDER.createOffer(dataAddressProperties);
        var objectName = UUID.randomUUID().toString();

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PUSH")
                .withDestination(createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "DataAddress")
                        .add(EDC_NAMESPACE + "keyName", "s3credentials")
                        .add(EDC_NAMESPACE + "type", "AmazonS3")
                        .add(EDC_NAMESPACE + "endpointOverride", CONSUMER_S3.getEndpoint().toString())
                        .add(EDC_NAMESPACE + "region", "eu-central-1")
                        .add(EDC_NAMESPACE + "bucketName", bucketName)
                        .add(EDC_NAMESPACE + "objectName", objectName)
                        .build())
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

        await().untilAsserted(() -> {
            var body = CONSUMER_S3.getS3Client().getObject(b -> b.bucket(bucketName).key(objectName)).readAllBytes();
            assertThat(body).asString().isEqualTo("data");
        });

        providerDataSource.stop();
    }

}
