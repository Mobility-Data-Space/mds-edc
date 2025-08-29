package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema;
import eu.dataspace.connector.extension.kafka.broker.openid.ClientRegistrationResponse;
import eu.dataspace.connector.extension.kafka.broker.openid.OpenIdConfiguration;
import eu.dataspace.connector.extension.kafka.broker.openid.OpenIdConnectService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Manages the authentication and the EDR creation for Kafka-PULL transfers
 */
class KafkaDataPlaneAuthorizationService implements DataPlaneAuthorizationService {
    private final OpenIdConnectService openIdConnectService;
    private final Vault vault;

    public KafkaDataPlaneAuthorizationService(OpenIdConnectService openIdConnectService, Vault vault) {
        this.openIdConnectService = openIdConnectService;
        this.vault = vault;
    }

    @Override
    public Result<DataAddress> createEndpointDataReference(DataFlow dataFlow) {
        var dataAddress = dataFlow.getSource();
        var discoveryUrl = dataAddress.getStringProperty(KafkaBrokerDataAddressSchema.OPENID_CONNECT_DISCOVERY_URL);
        var tokenKey = dataAddress.getStringProperty(KafkaBrokerDataAddressSchema.REGISTER_CLIENT_TOKEN_KEY);
        var token = vault.resolveSecret(tokenKey);

        ServiceResult<DataAddress> compose = openIdConnectService.fetchOpenIdConfiguration(discoveryUrl)
                .compose(configuration -> openIdConnectService.registerNewClient(configuration, token)
                        .compose(client -> {
                            var groupId = UUID.randomUUID().toString();

                            return openIdConnectService.userInfo(configuration, client)
                                    .map(userInfo -> {
                                        createAcls(dataAddress.getStringProperty(BOOTSTRAP_SERVERS), configuration.tokenEndpoint(),
                                                userCanAccess(userInfo.sub(), ResourceType.TOPIC, dataAddress.getStringProperty(TOPIC)),
                                                userCanAccess(userInfo.sub(), ResourceType.GROUP, groupId)
                                        );

                                        return createEdr(configuration, client, dataAddress, groupId);
                                    });

                        }));

        return compose
                .flatMap(result -> {
                    if (result.succeeded()) {
                        return Result.success(result.getContent());
                    } else {
                        return Result.failure(result.getFailureDetail());
                    }
                });

    }

    private DataAddress createEdr(OpenIdConfiguration configuration, ClientRegistrationResponse client, DataAddress dataAddress, String groupId) {
        return DataAddress.Builder.newInstance()
                .properties(dataAddress.getProperties()) // TODO: provisional, not all the properties should be sent!
                .property(EDC_NAMESPACE + "clientId", client.clientId())
                .property(EDC_NAMESPACE + "clientSecret", client.clientSecret())
                .property(EDC_NAMESPACE + "tokenEndpointUrl", configuration.tokenEndpoint())
                .property(EDC_NAMESPACE + "kafka.group.prefix", groupId)
                .build();
    }

    @Override
    public Result<DataAddress> authorize(String token, Map<String, Object> requestData) {
        return null;
    }

    @Override
    public ServiceResult<Void> revokeEndpointDataReference(String transferProcessId, String reason) {
        return null;
    }

    private void createAcls(String bootstrapServers, String tokenEndpointUrl, AclBinding... bindings) {
        var adminProperties = new Properties();
        // TODO: these should be passed in the address/vault
        adminProperties.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        adminProperties.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
        adminProperties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=\"%s\" clientSecret=\"%s\";"
                .formatted("myclient", "mysecret"));
        adminProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        adminProperties.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, OAuthBearerLoginCallbackHandler.class.getName());
        adminProperties.put(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, tokenEndpointUrl);
        try (var adminClient = AdminClient.create(adminProperties)) {
            adminClient.createAcls(Arrays.stream(bindings).toList()).all().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private AclBinding userCanAccess(String principalName, ResourceType resourceType, String resourceName) {
        var pattern = new ResourcePattern(resourceType, resourceName, PatternType.LITERAL);
        var entry = new AccessControlEntry("User:" + principalName, "*", AclOperation.READ, AclPermissionType.ALLOW);
        return new AclBinding(pattern, entry);
    }
}
