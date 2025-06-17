package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.S3Extension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class S3DataSourceTest {

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

    @Test
    void shouldSupportS3DataSourceTransfer() {
        var consumerDataDestination = startClientAndServer(getFreePort());
        consumerDataDestination.when(request("/destination")).respond(response());

        var fileContent = UUID.randomUUID().toString().getBytes();
        var bucketName = UUID.randomUUID().toString();
        var objectName = UUID.randomUUID().toString();

        var userAccessKey = PROVIDER_S3.createBucket(bucketName);
        PROVIDER_S3.uploadToBucket(bucketName, objectName, fileContent);

        PROVIDER.getService(Vault.class).storeSecret("s3credentials", Json.createObjectBuilder()
                .add("edctype", "dataspaceconnector:secrettoken")
                .add("accessKeyId", userAccessKey.accessKeyId())
                .add("secretAccessKey", userAccessKey.secretAccessKey())
                .build().toString());

        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "AmazonS3",
                EDC_NAMESPACE + "keyName", "s3credentials",
                EDC_NAMESPACE + "endpointOverride", PROVIDER_S3.getEndpoint().toString(),
                EDC_NAMESPACE + "region", "eu-central-1",
                EDC_NAMESPACE + "bucketName", bucketName,
                EDC_NAMESPACE + "objectName", objectName
        );

        var assetId = PROVIDER.createOffer(dataAddressProperties);

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PUSH")
                .withDestination(httpDataAddress("http://localhost:" + consumerDataDestination.getPort() + "/destination"))
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

        await().untilAsserted(() -> {
            consumerDataDestination.verify(request("/destination").withBody(binary(fileContent)));
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
