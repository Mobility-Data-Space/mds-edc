package eu.dataspace.connector.extension.kafka.broker.acls;

import eu.dataspace.connector.dataplane.kafka.spi.AccessControlLists;
import eu.dataspace.connector.dataplane.kafka.spi.AllowResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_ADMIN_PROPERTIES_KEY;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;

public class KafkaAccessControlLists implements AccessControlLists {

    public static final String KAFKA_PRINCIPAL_NAME_KEY_PREFIX = "kafka-principal-name-";
    private final Vault vault;
    // TODO: please note that this needs to change, likely upstream (likely revoke method should get the data address)
    private final ConcurrentHashMap<String, Properties> adminPropertiesCache = new ConcurrentHashMap<>();

    public KafkaAccessControlLists(Vault vault) {
        this.vault = vault;
    }

    @Override
    public ServiceResult<AllowResponse> allowAccessTo(String principalName, String dataFlowId, DataAddress dataAddress) {
        var adminPropertiesKey = dataAddress.getStringProperty(KAFKA_ADMIN_PROPERTIES_KEY);
        var adminProperties = vault.resolveSecret(adminPropertiesKey);
        var groupId = UUID.randomUUID().toString();

        return adminProperties(adminProperties)
                .compose(properties -> {
                    adminPropertiesCache.put(dataFlowId, properties);
                    return createAcls(properties,
                            userCanAccess(principalName, ResourceType.TOPIC, dataAddress.getStringProperty(TOPIC)),
                            userCanAccess(principalName, ResourceType.GROUP, groupId)
                    );
                })
                .onSuccess(a -> vault.storeSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + dataFlowId, principalName))
                .map(v -> new AllowResponse(groupId));
    }

    @Override
    public ServiceResult<Void> denyAccessTo(String dataFlowId) {
        var principalName = vault.resolveSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + dataFlowId);
        if (principalName == null) {
            return ServiceResult.notFound("Principal name not found for data flow " + dataFlowId);
        }

        var properties = adminPropertiesCache.get(dataFlowId);

        try (var adminClient = AdminClient.create(properties)) {
            adminClient.deleteAcls(List.of(new AclBindingFilter(
                    ResourcePatternFilter.ANY,
                    new AccessControlEntryFilter("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW)
            ))).all().get();
        } catch (ExecutionException | InterruptedException e) {
            return ServiceResult.unexpected("Cannot delete ACLs: " + e.getMessage());
        }

        vault.deleteSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + dataFlowId);

        return ServiceResult.success();
    }

    private ServiceResult<Void> createAcls(Properties adminProperties, AclBinding... bindings) {
        try (var adminClient = AdminClient.create(adminProperties)) {
            adminClient.createAcls(Arrays.stream(bindings).toList()).all().get();
        } catch (ExecutionException | InterruptedException e) {
            return ServiceResult.unexpected("Cannot create ACLs: " + e.getMessage());
        }

        return ServiceResult.success();
    }

    private AclBinding userCanAccess(String principalName, ResourceType resourceType, String resourceName) {
        var pattern = new ResourcePattern(resourceType, resourceName, PatternType.LITERAL);
        var entry = new AccessControlEntry("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW);
        return new AclBinding(pattern, entry);
    }

    private ServiceResult<Properties> adminProperties(String serializedProperties) {
        var properties = new Properties();
        try (var reader = new StringReader(serializedProperties)) {
            properties.load(reader);
        } catch (IOException e) {
            return ServiceResult.unexpected("Cannot get Kafka Admin properties: " + e.getMessage());
        }
        return ServiceResult.success(properties);
    }
}
