package eu.dataspace.connector.extension.kafka.broker.acls;

import eu.dataspace.connector.dataplane.kafka.spi.AccessControlLists;
import eu.dataspace.connector.dataplane.kafka.spi.AllowResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.KafkaFuture;
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
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_ADMIN_PROPERTIES_KEY;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;

public class KafkaAccessControlLists implements AccessControlLists {

    public static final String KAFKA_PRINCIPAL_NAME_KEY_PREFIX = "kafka-principal-name-";
    private final Vault vault;
    private final DataPlaneStore dataPlaneStore;

    public KafkaAccessControlLists(Vault vault, DataPlaneStore dataPlaneStore) {
        this.vault = vault;
        this.dataPlaneStore = dataPlaneStore;
    }

    @Override
    public ServiceResult<AllowResponse> allowAccessTo(String principalName, String dataFlowId, DataAddress dataAddress) {
        var groupId = UUID.randomUUID().toString();

        return onKafkaAdminDo(dataAddress, createAcls(
                userCanAccess(principalName, ResourceType.TOPIC, dataAddress.getStringProperty(TOPIC)),
                userCanAccess(principalName, ResourceType.GROUP, groupId)))
                .onSuccess(i -> vault.storeSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + dataFlowId, principalName))
                .map(v -> new AllowResponse(groupId));
    }

    @Override
    public ServiceResult<Void> denyAccessTo(String dataFlowId) {
        var principalName = vault.resolveSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + dataFlowId);
        if (principalName == null) {
            return ServiceResult.notFound("Principal name not found for data flow " + dataFlowId);
        }

        var dataFlow = dataPlaneStore.findById(dataFlowId);
        if (dataFlow == null) {
            return ServiceResult.notFound("Cannot retrieve DataFlow %s to deny access".formatted(dataFlowId));
        }

        return onKafkaAdminDo(dataFlow.getActualSource(), deleteAcls(anyAllowReadPermission(principalName)))
                .onSuccess(i -> vault.deleteSecret(KAFKA_PRINCIPAL_NAME_KEY_PREFIX + dataFlowId));
    }

    private ServiceResult<Void> onKafkaAdminDo(DataAddress dataAddress, Function<AdminClient, KafkaFuture<?>> action) {
        var adminPropertiesKey = dataAddress.getStringProperty(KAFKA_ADMIN_PROPERTIES_KEY);
        if (adminPropertiesKey == null) {
            return ServiceResult.unexpected("Source DataAddress doesn't contain mandatory key " + KAFKA_ADMIN_PROPERTIES_KEY);
        }
        var plainProperties = vault.resolveSecret(adminPropertiesKey);
        if (plainProperties == null) {
            return ServiceResult.unexpected("Cannot get Kafka Admin properties from Vault key " + adminPropertiesKey);
        }

        var adminProperties = new Properties();
        try (var reader = new StringReader(plainProperties)) {
            adminProperties.load(reader);
        } catch (IOException e) {
            return ServiceResult.unexpected("Cannot parse Kafka Admin properties: " + e.getMessage());
        }

        try (var adminClient = AdminClient.create(adminProperties)) {
            action.apply(adminClient).get();
            return ServiceResult.success();
        } catch (Exception e) {
            return ServiceResult.unexpected("Cannot create ACLs: " + e.getMessage());
        }
    }

    private Function<AdminClient, KafkaFuture<?>> createAcls(AclBinding... bindings) {
        return adminClient -> adminClient.createAcls(Arrays.stream(bindings).toList()).all();
    }

    private Function<AdminClient, KafkaFuture<?>> deleteAcls(AclBindingFilter... bindingFilters) {
        return adminClient -> adminClient.deleteAcls(Arrays.stream(bindingFilters).toList()).all();
    }

    private AclBinding userCanAccess(String principalName, ResourceType resourceType, String resourceName) {
        var pattern = new ResourcePattern(resourceType, resourceName, PatternType.LITERAL);
        var entry = new AccessControlEntry("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW);
        return new AclBinding(pattern, entry);
    }

    private @NotNull AclBindingFilter anyAllowReadPermission(String principalName) {
        return new AclBindingFilter(
                ResourcePatternFilter.ANY,
                new AccessControlEntryFilter("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW)
        );
    }

}
