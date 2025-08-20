package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.extension.kafka.broker.auth.OpenIdConnectService;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Kafka Broker flow extension
 */
@Extension(value = KafkaBrokerExtension.NAME)
public class KafkaBrokerExtension implements ServiceExtension {

    public static final String NAME = "Kafka stream extension";
    private static final String DEFAULT_DATAPLANE_SELECTOR_STRATEGY = "random";
    @Setting(value = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime", defaultValue = DEFAULT_DATAPLANE_SELECTOR_STRATEGY)
    private static final String DPF_SELECTOR_STRATEGY = "edc.dataplane.client.selector.strategy";

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private PublicEndpointGeneratorService publicEndpointGeneratorService;

    @Inject
    private PipelineService pipelineService;

    @Override
    public void initialize(final ServiceExtensionContext context) {
        var selectionStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, DEFAULT_DATAPLANE_SELECTOR_STRATEGY);
        var kafkaOAuthService = new OpenIdConnectService(httpClient, typeManager.getMapper());
//        var controller = new KafkaBrokerDataFlowController(vault, kafkaOAuthService, transferTypeParser, getPropertiesProvider(),
//                selectorService, clientFactory, selectionStrategy, monitor, callbackUrl);
//        dataFlowManager.register(controller);
        context.registerService(OpenIdConnectService.class, kafkaOAuthService);

        pipelineService.registerFactory(new KafkaDummySourceFactory());

        publicEndpointGeneratorService.addGeneratorFunction("Kafka", address -> {
            return Endpoint.url("cueo");
        });
    }

    /**
     * TODO: this is necessary because in the data-plane self registration the allowed sources are decided by the `PipelineService`
     * this will likely need some work upstream
     */
    private static class KafkaDummySourceFactory implements DataSourceFactory {
        @Override
        public String supportedType() {
            return "KafkaBroker";
        }

        @Override
        public DataSource createSource(DataFlowStartMessage dataFlowStartMessage) {
            return null;
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowStartMessage dataFlowStartMessage) {
            return null;
        }
    }
}
