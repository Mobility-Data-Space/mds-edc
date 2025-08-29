package eu.dataspace.connector.extension.kafka.broker;

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
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_CLIENT_ID;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_CLIENT_SECRET;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_DISCOVERY_URL;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_REGISTER_CLIENT_TOKEN_KEY;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.OIDC_TOKEN_ENDPOINT;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.SASL_MECHANISM;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.SECURITY_PROTOCOL;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;

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
        var discoveryUrl = dataAddress.getStringProperty(OIDC_DISCOVERY_URL);
        var tokenKey = dataAddress.getStringProperty(OIDC_REGISTER_CLIENT_TOKEN_KEY);
        var token = vault.resolveSecret(tokenKey);

        ServiceResult<DataAddress> compose = openIdConnectService.fetchOpenIdConfiguration(discoveryUrl)
                .compose(configuration -> openIdConnectService.registerNewClient(configuration, token)
                        .compose(client -> openIdConnectService.userInfo(configuration, client)
                                .map(userInfo -> {
                                    var groupId = UUID.randomUUID().toString();

                                    createAcls(dataAddress.getStringProperty(BOOTSTRAP_SERVERS), configuration.tokenEndpoint(),
                                            userCanAccess(userInfo.sub(), ResourceType.TOPIC, dataAddress.getStringProperty(TOPIC)),
                                            userCanAccess(userInfo.sub(), ResourceType.GROUP, groupId)
                                    );

                                    return createEdr(configuration, client, dataAddress, groupId);
                                })));

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
                .type(EDR_SIMPLE_TYPE)
                .property(OIDC_CLIENT_ID, client.clientId())
                .property(OIDC_CLIENT_SECRET, client.clientSecret())
                .property(OIDC_TOKEN_ENDPOINT, configuration.tokenEndpoint())
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
