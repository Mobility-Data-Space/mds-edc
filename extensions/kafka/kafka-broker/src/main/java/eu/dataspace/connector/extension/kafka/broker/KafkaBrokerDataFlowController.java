package eu.dataspace.connector.extension.kafka.broker;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import eu.dataspace.connector.extension.kafka.broker.auth.KafkaOAuthService;
import eu.dataspace.connector.extension.kafka.broker.auth.OAuthCredentials;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.CONTRACT_ID;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;

/**
 * Implementation of the {@link DataFlowController} interface responsible for managing data flows
 * using Kafka.
 * It integrates with OAuth for token management and authorizes data transfer operations.
 */
class KafkaBrokerDataFlowController implements DataFlowController {
    public static final String DEFAULT_POLL_DURATION = Duration.ofSeconds(1).toString();
    static final String START_FAILED = "Failed to start data flow: ";
    static final String SUSPEND_FAILED = "Failed to suspend data flow: ";
    static final String TERMINATE_FAILED = "Failed to terminate data flow: ";
    static final String SECRET_NOT_DEFINED = "Secret key %s was not defined";
    static final String DESTINATION_TYPE = "HttpData";
    private static final String TRANSFER_TYPE = "Kafka-PULL";

    private final Vault vault;
    private final KafkaOAuthService oauthService;
    private final TransferTypeParser transferTypeParser;
    private final DataFlowPropertiesProvider propertiesProvider;
    private final DataPlaneSelectorService selectorService;
    private final DataPlaneClientFactory clientFactory;
    private final String selectionStrategy;
    private final Monitor monitor;
    private final ControlApiUrl callbackUrl;

    public KafkaBrokerDataFlowController(final Vault vault, final KafkaOAuthService oauthService,
                                         TransferTypeParser transferTypeParser, DataFlowPropertiesProvider propertiesProvider, DataPlaneSelectorService selectorService,
                                         DataPlaneClientFactory clientFactory, String selectionStrategy, Monitor monitor, ControlApiUrl callbackUrl) {
        this.vault = vault;
        this.oauthService = oauthService;
        this.transferTypeParser = transferTypeParser;
        this.propertiesProvider = propertiesProvider;
        this.selectorService = selectorService;
        this.clientFactory = clientFactory;
        this.selectionStrategy = selectionStrategy;
        this.monitor = monitor;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public boolean canHandle(final TransferProcess transferProcess) {
        if(TRANSFER_TYPE.equals(transferProcess.getTransferType())) {
            if(transferProcess.getContentDataAddress() != null){
                return KAFKA_TYPE.equals(transferProcess.getContentDataAddress().getType());
            }
        }
        return false ;
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(final TransferProcess transferProcess, final Policy policy) {
        var transferTypeParse = transferTypeParser.parse(transferProcess.getTransferType());
        if (transferTypeParse.failed()) {
            return StatusResult.failure(FATAL_ERROR, transferTypeParse.getFailureDetail());
        }

        var propertiesResult = propertiesProvider.propertiesFor(transferProcess, policy);
        if (propertiesResult.failed()) {
            return StatusResult.failure(FATAL_ERROR, propertiesResult.getFailureDetail());
        }

        var contentDataAddress = transferProcess.getContentDataAddress();

        String token;
        try {
            OAuthCredentials creds = extractOAuthCredentials(contentDataAddress);
            token = oauthService.getAccessToken(creds);
        } catch (EdcException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, START_FAILED + e.getMessage());
        }
        vault.storeSecret(transferProcess.getId(), token);

        Map<String, String> content = buildProperties(transferProcess, policy, contentDataAddress, propertiesResult, token);
        // @TODO: Review for DPS and DataPlaneAuthorizationService with front-channel and back-channel concepts
        var dataFlowRequest = DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(transferProcess.getId())
                .sourceDataAddress(transferProcess.getContentDataAddress())
                .destinationDataAddress(transferProcess.getDataDestination())
                .participantId(policy.getAssignee())
                .agreementId(transferProcess.getContractId())
                .assetId(transferProcess.getAssetId())
                .transferType(new TransferType(DESTINATION_TYPE, FlowType.PULL))
                .callbackAddress(callbackUrl != null ? callbackUrl.get() : null)
                .properties(content)
                .build();

        var dataPlaneInstance = selectorService.getAll().getContent().getFirst();
        return clientFactory.createClient(dataPlaneInstance)
                .start(dataFlowRequest)
                .map(it -> DataFlowResponse.Builder.newInstance()
                        .dataAddress(it.getDataAddress())
                        .dataPlaneId(dataPlaneInstance.getId())
                        .build()
                );
    }

    private @NotNull Map<String, String> buildProperties(TransferProcess transferProcess, Policy policy, DataAddress contentDataAddress, StatusResult<Map<String, String>> propertiesResult, String token) {
        var pollDuration = Optional.ofNullable(contentDataAddress.getStringProperty(POLL_DURATION)).orElse(DEFAULT_POLL_DURATION);
        Map<String, String> content = new HashMap<>(propertiesResult.getContent());
        content.put(TOKEN, token);
        content.put(BOOTSTRAP_SERVERS, contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS));
        content.put(CONTRACT_ID, transferProcess.getContractId());
        content.put(TOPIC, contentDataAddress.getStringProperty(TOPIC));
        content.put(PROTOCOL, contentDataAddress.getStringProperty(PROTOCOL));
        content.put(MECHANISM, contentDataAddress.getStringProperty(MECHANISM));
        content.put(GROUP_PREFIX, policy.getAssignee());
        content.put(POLL_DURATION, pollDuration);
        return content;
    }

    private OAuthCredentials extractOAuthCredentials(final DataAddress contentDataAddress) {
        var tokenUrl = contentDataAddress.getStringProperty(OAUTH_TOKEN_URL);
        var revokeUrl = Optional.ofNullable(contentDataAddress.getStringProperty(OAUTH_REVOKE_URL));
        var clientId = contentDataAddress.getStringProperty(OAUTH_CLIENT_ID);
        var clientSecret = getSecret(contentDataAddress.getStringProperty(OAUTH_CLIENT_SECRET_KEY));

        return new OAuthCredentials(tokenUrl, revokeUrl, clientId, clientSecret);
    }

    @Override
    public StatusResult<Void> suspend(final TransferProcess transferProcess) {
        return deleteCredentialsAndRevokeAccess(transferProcess, SUSPEND_FAILED);
    }

    @Override
    public StatusResult<Void> terminate(final TransferProcess transferProcess) {
        return deleteCredentialsAndRevokeAccess(transferProcess, TERMINATE_FAILED);
    }

    @Override
    public Set<String> transferTypesFor(final Asset asset) {
        return Set.of(TRANSFER_TYPE);
    }

    @Override
    public StatusResult<DataFlowResponse> provision(TransferProcess tp, Policy p) {
        return null ;
    }

    private String getSecret(final String secretKey) {
        return Optional.ofNullable(vault.resolveSecret(secretKey)).orElseThrow(() -> new EdcException(SECRET_NOT_DEFINED.formatted(secretKey)));
    }

    private StatusResult<Void> deleteCredentialsAndRevokeAccess(final TransferProcess transferProcess, final String error) {
        try {
            var contentDataAddress = transferProcess.getContentDataAddress();
            var transferProcessId = transferProcess.getId();

            OAuthCredentials creds = extractOAuthCredentials(contentDataAddress);
            String token = getSecret(transferProcessId);

            oauthService.revokeToken(creds, token);
            vault.deleteSecret(transferProcessId);

            return StatusResult.success();

        } catch (EdcException e) {
            return StatusResult.failure(FATAL_ERROR, error + e.getMessage());
        }
    }
}