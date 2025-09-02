package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.dataplane.kafka.spi.Credentials;
import eu.dataspace.connector.dataplane.kafka.spi.IdentityProvider;
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
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_ADMIN_PROPERTIES_KEY;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_CLIENT_ID;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_CLIENT_SECRET;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_TOKEN_ENDPOINT;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.SASL_MECHANISM;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.SECURITY_PROTOCOL;
import static eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;

/**
 * Manages the authentication and the EDR creation for Kafka-PULL transfers
 */
class KafkaDataPlaneAuthorizationService implements DataPlaneAuthorizationService {
    private final IdentityProvider identityProvider;
    private final Vault vault;
    private final ConcurrentHashMap<String, Properties> adminPropertiesCache = new ConcurrentHashMap<>(); // TODO: please note that this needs to change, likely upstream (likey revoke method should get the data address)

    public KafkaDataPlaneAuthorizationService(IdentityProvider identityProvider, Vault vault) {
        this.identityProvider = identityProvider;
        this.vault = vault;
    }

    @Override
    public Result<DataAddress> createEndpointDataReference(DataFlow dataFlow) {
        var dataAddress = dataFlow.getSource();

        return identityProvider.grantAccess(dataAddress)
                .compose(credentials -> {
                    var groupId = UUID.randomUUID().toString();
                    var adminPropertiesKey = dataAddress.getStringProperty(KAFKA_ADMIN_PROPERTIES_KEY);
                    var adminProperties = vault.resolveSecret(adminPropertiesKey);

                    ServiceResult<DataAddress> edr = adminProperties(adminProperties)
                            .compose(properties -> {
                                adminPropertiesCache.put(dataFlow.getId(), properties);
                                return createAcls(properties,
                                        userCanAccess(credentials.subject(), ResourceType.TOPIC, dataAddress.getStringProperty(TOPIC)),
                                        userCanAccess(credentials.subject(), ResourceType.GROUP, groupId)
                                );
                            })
                            .map(v -> createEdr(credentials, dataAddress, groupId));
                    return edr
                            .onSuccess(a -> vault.storeSecret("kafka-principal-name-" + dataFlow.getId(), credentials.subject()));
                })
                .flatMap(result -> {
                    if (result.succeeded()) {
                        return Result.success(result.getContent());
                    } else {
                        return Result.failure(result.getFailureDetail());
                    }
                });
    }

    private DataAddress createEdr(Credentials credentials, DataAddress dataAddress, String groupId) {
        return DataAddress.Builder.newInstance()
                .type(EDR_SIMPLE_TYPE)
                .property(OIDC_CLIENT_ID, credentials.clientId())
                .property(OIDC_CLIENT_SECRET, credentials.clientSecret())
                .property(OIDC_TOKEN_ENDPOINT, credentials.tokenEndpoint())
                .property(GROUP_PREFIX, groupId)
                .property(TOPIC, dataAddress.getStringProperty(TOPIC))
                .property(BOOTSTRAP_SERVERS, dataAddress.getStringProperty(BOOTSTRAP_SERVERS))
                .property(SECURITY_PROTOCOL, dataAddress.getStringProperty(SECURITY_PROTOCOL))
                .property(SASL_MECHANISM, dataAddress.getStringProperty(SASL_MECHANISM))
                .build();
    }

    @Override
    public Result<DataAddress> authorize(String token, Map<String, Object> requestData) {
        return null;
    }

    @Override
    public ServiceResult<Void> revokeEndpointDataReference(String transferProcessId, String reason) {
        var principalName = vault.resolveSecret("kafka-principal-name-" + transferProcessId);
        if (principalName == null) {
            return ServiceResult.notFound("Principal name not found for data flow " + transferProcessId);
        }

        var properties = adminPropertiesCache.get(transferProcessId);

        try (var adminClient = AdminClient.create(properties)) {
            adminClient.deleteAcls(List.of(new AclBindingFilter(
                    ResourcePatternFilter.ANY,
                    new AccessControlEntryFilter("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW)
            ))).all().get();
        } catch (ExecutionException | InterruptedException e) {
            return ServiceResult.unexpected("Cannot delete ACLs: " + e.getMessage());
        }

        vault.deleteSecret("kafka-principal-name-" + transferProcessId);

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

    private ServiceResult<Properties> adminProperties(String serializedProperties) {
        var properties = new Properties();
        try (var reader = new StringReader(serializedProperties)) {
            properties.load(reader);
        } catch (IOException e) {
            return ServiceResult.unexpected("Cannot get Kafka Admin properties: " + e.getMessage());
        }
        return ServiceResult.success(properties);
    }

    private AclBinding userCanAccess(String principalName, ResourceType resourceType, String resourceName) {
        var pattern = new ResourcePattern(resourceType, resourceName, PatternType.LITERAL);
        var entry = new AccessControlEntry("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW);
        return new AclBinding(pattern, entry);
    }
}
