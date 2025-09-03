package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.dataplane.kafka.spi.AccessControlLists;
import eu.dataspace.connector.dataplane.kafka.spi.IdentityProvider;
import eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

@Extension(value = DataPlaneKafkaExtension.NAME)
public class DataPlaneKafkaExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Kafka";

    @Inject
    private PublicEndpointGeneratorService publicEndpointGeneratorService;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private IdentityProvider identityProvider;
    @Inject
    private AccessControlLists accessControlLists;

    @Override
    public void initialize(final ServiceExtensionContext context) {
        // TODO(upstream): this is necessary because in the data-plane self registration the allowed sources are decided by the `PipelineService` this will likely need some work upstream
        pipelineService.registerFactory(new KafkaDummySourceFactory());

        // TODO(upstream): this is necessary because currently the public endpoint generator service is deciding which pull
        //  transfers are supported.
        publicEndpointGeneratorService.addGeneratorFunction("Kafka", address -> null);
    }

    @Provider
    public DataPlaneAuthorizationService dataPlaneAuthorizationService() {
        return new DataPlaneKafkaAuthorizationService(identityProvider, accessControlLists);
    }

    private static class KafkaDummySourceFactory implements DataSourceFactory {
        @Override
        public String supportedType() {
            return KafkaBrokerDataAddressSchema.KAFKA_TYPE;
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
