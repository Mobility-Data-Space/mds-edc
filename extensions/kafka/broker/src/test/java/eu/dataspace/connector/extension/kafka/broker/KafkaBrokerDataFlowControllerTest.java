package eu.dataspace.connector.extension.kafka.broker;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import eu.dataspace.connector.extension.kafka.broker.auth.KafkaOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;
import static eu.dataspace.connector.extension.kafka.broker.KafkaBrokerDataFlowController.*;
import static org.mockito.Mockito.*;

class KafkaBrokerDataFlowControllerTest {
    public static final String NOT_DEFINED_SECRET_KEY = "clientSecretKey";
    public static final String SECRET_KEY = "secret-key";
    private final Vault vault = mock();
    private final KafkaOAuthService oauthService = mock();
    private final DataPlaneClient dataPlaneClient = mock();
    private final DataPlaneClientFactory dataPlaneClientFactory = mock();
    private final DataPlaneSelectorService selectorService = mock();
    private final DataFlowPropertiesProvider propertiesProvider = mock();
    private final TransferTypeParser transferTypeParser = mock();
    private KafkaBrokerDataFlowController controller;
    private DataAddress contentDataAddress;
    private TransferProcess transferProcess;
    private Policy policy;

    @BeforeEach
    void setUp() {
        contentDataAddress = DataAddress.Builder.newInstance()
                .type(KAFKA_TYPE)
                .property(TOPIC, "test-topic")
                .property(BOOTSTRAP_SERVERS, "localhost:9092")
                .property(PROTOCOL, "protocol")
                .property(MECHANISM, "mechanism")
                .property(GROUP_PREFIX, "test-group")
                .property(POLL_DURATION, "PT5M")
                .property(OAUTH_TOKEN_URL, "http://localhost:8080/token")
                .property(OAUTH_REVOKE_URL, "http://keycloak:8080/revoke")
                .property(OAUTH_CLIENT_ID, "client-id")
                .property(OAUTH_CLIENT_SECRET_KEY, NOT_DEFINED_SECRET_KEY)
                .property(TOKEN, "token")
                .build();

        transferProcess = TransferProcess.Builder.newInstance()
                .contentDataAddress(contentDataAddress)
                .transferType("Kafka-PULL")
                .contractId("contract")
                .correlationId("correlation")
                .id("transferProcessId").build();
        policy = Policy.Builder.newInstance().assignee("test-group").build();

        when(oauthService.getAccessToken(any())).thenReturn("token");

        when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Kafka", FlowType.PULL)));

        Map<String, Object> properties = contentDataAddress.getProperties();
        when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()))));

        var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://any").build();
        when(selectorService.select(any(), anyString(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
        when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance)));

        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

        var response = mock(DataFlowResponseMessage.class);
        when(response.getDataAddress()).thenReturn(contentDataAddress);
        when(dataPlaneClient.start(any(DataFlowStartMessage.class))).thenReturn(StatusResult.success(response));

        controller = new KafkaBrokerDataFlowController(vault, oauthService, transferTypeParser, propertiesProvider,
                selectorService, dataPlaneClientFactory, "random", new ConsoleMonitor(), () -> URI.create("http://localhost"));
    }

    @Test
    void canHandle_ShouldReturnTrue_WhenTypeAndTransferTypeMatch() {
        boolean result = controller.canHandle(transferProcess);
        assertThat(result).isTrue();
    }

    @Test
    void canHandle_ShouldReturnFalse_WhenTypeDoesNotMatch() {
        contentDataAddress.setType("Non-Kafka");
        transferProcess.setContentDataAddress(contentDataAddress);
        boolean result = controller.canHandle(transferProcess);
        assertThat(result).isFalse();
    }

    @Test
    void canHandle_ShouldReturnFalse_WhenTransferTypeDoesNotMatch() {
        transferProcess = TransferProcess.Builder.newInstance().contentDataAddress(contentDataAddress).transferType("Not-Kafka-PULL").build();
        boolean result = controller.canHandle(transferProcess);
        assertThat(result).isFalse();
    }

    @Test
    void start_ShouldReturnSuccess_WhenValidInput() {
        when(vault.resolveSecret(any())).thenReturn(SECRET_KEY);
        StatusResult<DataFlowResponse> result = controller.start(transferProcess, policy);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();

        AbstractResultAssert.assertThat(result).isSucceeded().extracting(DataFlowResponse::getDataAddress).satisfies(kafkaDataAddress -> {
            assertThat(kafkaDataAddress.getType()).isEqualTo(KAFKA_TYPE);
            assertThat(kafkaDataAddress.getStringProperty(BOOTSTRAP_SERVERS)).isEqualTo("localhost:9092");
            assertThat(kafkaDataAddress.getStringProperty(TOPIC)).isEqualTo("test-topic");
            assertThat(kafkaDataAddress.getStringProperty(GROUP_PREFIX)).isEqualTo("test-group");
            assertThat(kafkaDataAddress.getStringProperty(MECHANISM)).isEqualTo("mechanism");
            assertThat(kafkaDataAddress.getStringProperty(PROTOCOL)).isEqualTo("protocol");
            assertThat(kafkaDataAddress.getStringProperty(TOKEN)).isEqualTo("token");
        });
    }

    @Test
    void start_ShouldReturnFailure_WhenMissedSecret() {
        StatusResult<?> result = controller.start(transferProcess, policy);
        assertThat(result.fatalError()).isTrue();
        assertThat(START_FAILED + SECRET_NOT_DEFINED.formatted(NOT_DEFINED_SECRET_KEY)).isEqualTo(result.getFailureDetail());
    }

    @Test
    void suspend_ShouldReturnSuccess_WhenOperationSucceeds() {
        when(vault.resolveSecret(any())).thenReturn(SECRET_KEY);
        StatusResult<?> result = controller.suspend(transferProcess);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void suspend_ShouldReturnFailure_WhenMissedSecret() {
        StatusResult<?> result = controller.suspend(transferProcess);
        assertThat(result.fatalError()).isTrue();
        assertThat(SUSPEND_FAILED + SECRET_NOT_DEFINED.formatted(NOT_DEFINED_SECRET_KEY)).isEqualTo(result.getFailureDetail());
    }

    @Test
    void terminate_ShouldReturnSuccess_WhenOperationSucceeds() {
        when(vault.resolveSecret(any())).thenReturn(SECRET_KEY);
        StatusResult<?> result = controller.terminate(transferProcess);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void terminate_ShouldReturnFailure_WhenMissedSecret() {
        StatusResult<?> result = controller.terminate(transferProcess);
        assertThat(result.fatalError()).isTrue();
        assertThat(TERMINATE_FAILED + SECRET_NOT_DEFINED.formatted(NOT_DEFINED_SECRET_KEY)).isEqualTo(result.getFailureDetail());
    }
}

