package eu.dataspace.connector.tests.feature;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.extensions.AzuriteExtension;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.SovityDapsExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class AzureBlobStorageTransferTest {

    @RegisterExtension
    @Order(0)
    private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

    @RegisterExtension
    @Order(1)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("provider", "consumer");

    @RegisterExtension
    @Order(2)
    private static final SovityDapsExtension DAPS_EXTENSION = new SovityDapsExtension();

    private static final AzuriteExtension.Account CONSUMER_AZURITE_ACCOUNT = new AzuriteExtension.Account("consumer", "key1");
    private static final AzuriteExtension.Account PROVIDER_AZURITE_ACCOUNT = new AzuriteExtension.Account("provider", "key2");

    @RegisterExtension
    @Order(3)
    private static final AzuriteExtension AZURITE_EXTENSION = new AzuriteExtension(CONSUMER_AZURITE_ACCOUNT, PROVIDER_AZURITE_ACCOUNT);

    @RegisterExtension
    @Order(4)
    private static final MdsParticipant PROVIDER = MdsParticipantFactory.hashicorpVault("provider", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
            .configurationProvider(AZURITE_EXTENSION::getConfig);

    @RegisterExtension
    @Order(4)
    private static final MdsParticipant CONSUMER = MdsParticipantFactory.hashicorpVault("consumer", VAULT_EXTENSION, DAPS_EXTENSION, POSTGRES_EXTENSION)
            .configurationProvider(AZURITE_EXTENSION::getConfig);

    @Test
    void transfer() {
        var sourceContainer = UUID.randomUUID().toString();
        var sourceObject = "source-blob";
        var sourceContainerClient = AZURITE_EXTENSION.getClient(PROVIDER_AZURITE_ACCOUNT)
                .createBlobContainer(sourceContainer);
        var data = "Really important data";
        sourceContainerClient
                .getBlobClient(sourceObject)
                .upload(BinaryData.fromString(data));

        PROVIDER.getService(Vault.class).storeSecret("azuriteCredentialsProvider", PROVIDER_AZURITE_ACCOUNT.key());

        Map<String, Object> dataAddressProperties = Map.of(
                EDC_NAMESPACE + "type", "AzureStorage",
                EDC_NAMESPACE + "keyName", "azuriteCredentialsProvider",
                EDC_NAMESPACE + "account", PROVIDER_AZURITE_ACCOUNT.name(),
                EDC_NAMESPACE + "container", sourceContainer,
                EDC_NAMESPACE + "blobName", sourceObject
        );

        var assetId = PROVIDER.createOffer(dataAddressProperties);

        var destinationContainer = UUID.randomUUID().toString();
        var destinationObject = "destination_object";
        var destinationContainerClient = AZURITE_EXTENSION.getClient(CONSUMER_AZURITE_ACCOUNT)
                .createBlobContainer(destinationContainer);

        CONSUMER.getService(Vault.class).storeSecret("azuriteCredentialsConsumer", generateSasToken(destinationContainerClient).toString());

        var destination = createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "AzureStorage")
                .add(EDC_NAMESPACE + "keyName", "azuriteCredentialsConsumer")
                .add(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name())
                .add(EDC_NAMESPACE + "container", destinationContainer)
                .add(EDC_NAMESPACE + "blobName", destinationObject)
                .build();

        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("AzureStorage-PUSH")
                .withDestination(destination)
                .execute();

        CONSUMER.awaitTransferToBeInState(transferProcessId, COMPLETED);

        assertThat(destinationContainerClient.getBlobClient(destinationObject).downloadContent().toString()).isEqualTo(data);
    }

    private JsonObject generateSasToken(BlobContainerClient containerClient) {
        var expiryTime = OffsetDateTime.now().plusMinutes(1);
        var signatureValues = new BlobServiceSasSignatureValues(expiryTime, BlobContainerSasPermission.parse("w"));
        var providerSas = containerClient.generateSas(signatureValues);

        return createObjectBuilder()
                .add("edctype", "dataspaceconnector:azuretoken")
                .add("sas", "?" +   providerSas)
                .add("expiration", expiryTime.toInstant().toEpochMilli())
                .build();
    }

}
