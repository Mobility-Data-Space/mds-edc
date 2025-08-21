package eu.dataspace.connector.extension.kafka.broker;

import eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema;
import eu.dataspace.connector.extension.kafka.broker.auth.OpenIdConnectService;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static eu.dataspace.connector.extension.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.PROTOCOL;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Extension(value = KafkaBrokerExtension.NAME)
public class KafkaBrokerExtension implements ServiceExtension {

    public static final String NAME = "Kafka stream extension";

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private PublicEndpointGeneratorService publicEndpointGeneratorService;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;

    @Inject
    private Vault vault;

    @Inject
    private ProvisionerManager provisionerManager;

    @Override
    public void initialize(final ServiceExtensionContext context) {
        pipelineService.registerFactory(new KafkaDummySourceFactory());

        publicEndpointGeneratorService.addGeneratorFunction("Kafka", address -> {
            // TODO: are these correct?
            return new Endpoint(address.getStringProperty(BOOTSTRAP_SERVERS), address.getStringProperty(PROTOCOL));
        });

        resourceDefinitionGeneratorManager.registerProviderGenerator(new ResourceDefinitionGenerator() {
            @Override
            public String supportedType() {
                return "Kafka";
            }

            @Override
            public ProvisionResource generate(DataFlow dataFlow) {
                return ProvisionResource.Builder.newInstance()
                        .type("KafkaOauth2")
                        .flowId(dataFlow.getId())
                        .dataAddress(dataFlow.getSource())
                        .build();
            }
        });

        provisionerManager.register(new Provisioner() {

            private final OpenIdConnectService openIdConnectService = new OpenIdConnectService(httpClient, typeManager.getMapper());

            @Override
            public String supportedType() {
                return "KafkaOauth2";
            }

            @Override
            public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResource provisionResource) {
                var dataAddress = provisionResource.getDataAddress();
                var discoveryUrl = dataAddress.getStringProperty(KafkaBrokerDataAddressSchema.OPENID_CONNECT_DISCOVERY_URL);
                var tokenKey = dataAddress.getStringProperty(KafkaBrokerDataAddressSchema.REGISTER_CLIENT_TOKEN_KEY);
                var token = vault.resolveSecret(tokenKey);
                var provisionedResource = openIdConnectService.registerNewClient(discoveryUrl, token)
                        .map(response -> {
                            var newAddress = DataAddress.Builder.newInstance()
                                    .properties(dataAddress.getProperties())
                                    .property(EDC_NAMESPACE + "clientId", response.clientId())
                                    .build();

                            return ProvisionedResource.Builder.from(provisionResource).dataAddress(newAddress).build();
                        })
                        .flatMap(result -> {
                            if (result.succeeded()) {
                                return StatusResult.success(result.getContent());
                            } else {
                                return StatusResult.failure(ResponseStatus.FATAL_ERROR, result.getFailureDetail());
                            }
                        });

                return CompletableFuture.completedFuture(provisionedResource);
            }
        });
    }

    /**
     * TODO: this is necessary because in the data-plane self registration the allowed sources are decided by the `PipelineService`
     * this will likely need some work upstream
     */
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
