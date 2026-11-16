package eu.dataspace.connector.dataplane.api;

import eu.dataspace.connector.dataplane.api.service.DataFlowServiceImpl;
import eu.dataspace.connector.dataplane.api.transform.JsonObjectFromDataFlowTransformer;
import eu.dataspace.connector.dataplane.api.v4.DataFlowApiV4Controller;
import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static eu.dataspace.connector.dataplane.api.DataFlowManagementApiExtension.NAME;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_V_4;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class DataFlowManagementApiExtension implements ServiceExtension {

    public static final String NAME = "DataFlow Management API";

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonObjectValidatorRegistry validator;
    @Inject
    private DataPlaneStore dataPlaneStore;
    @Inject
    private TransferProcessStore transferProcessStore;
    @Inject
    private DataAddressStore dataAddressStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private Monitor monitor;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        var managementTypeTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        var managementTypeTransformerRegistryV4 = managementTypeTransformerRegistry.forContext(MANAGEMENT_API_V_4);

        managementTypeTransformerRegistry.register(new JsonObjectFromDataFlowTransformer(jsonFactory));

        var service = new DataFlowServiceImpl(dataPlaneStore, transferProcessStore, dataAddressStore, transactionContext);

        webService.registerResource(ApiContext.MANAGEMENT, new DataFlowApiV4Controller(service, managementTypeTransformerRegistryV4, validator, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, DataFlowApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4));
    }
}
