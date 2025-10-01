package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.dataplane.kafka.spi.AccessControlLists;
import eu.dataspace.connector.dataplane.kafka.spi.IdentityProvider;
import eu.dataspace.connector.dataplane.kafka.spi.KafkaBrokerDataAddressSchema;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

@Extension(value = DataPlaneKafkaExtension.NAME)
public class DataPlaneKafkaExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Kafka";

    @Inject
    private PipelineService pipelineService;
    @Inject
    private IdentityProvider identityProvider;
    @Inject
    private AccessControlLists accessControlLists;
    @Inject
    private EndpointDataReferenceServiceRegistry endpointDataReferenceServiceRegistry;

    @Override
    public void initialize(final ServiceExtensionContext context) {
        // TODO(upstream): this is necessary because in the data-plane self registration the allowed sources are decided by the `PipelineService` this will likely need some work upstream
        pipelineService.registerFactory(new KafkaDummySourceFactory());

        endpointDataReferenceServiceRegistry.register("Kafka", new KafkaEndpointDataReferenceService(identityProvider, accessControlLists));
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
