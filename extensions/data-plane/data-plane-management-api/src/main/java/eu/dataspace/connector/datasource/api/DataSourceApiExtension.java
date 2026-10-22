package eu.dataspace.connector.datasource.api;

import eu.dataspace.connector.datasource.api.transform.JsonObjectFromDataSourceTransformer;
import eu.dataspace.connector.datasource.api.transform.JsonObjectToDataSourceTransformer;
import eu.dataspace.connector.datasource.api.v4.DataSourceApiV4Controller;
import eu.dataspace.connector.datasource.spi.service.DataSourceService;
import jakarta.json.Json;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static eu.dataspace.connector.datasource.api.DataSourceApiExtension.NAME;
import static eu.dataspace.connector.datasource.spi.types.DataSource.DATA_SOURCE_TYPE;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_V_4;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = NAME)
public class DataSourceApiExtension implements ServiceExtension {

    public static final String NAME = "Data Source API";

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonObjectValidatorRegistry validator;
    @Inject
    private DataSourceService dataSourceService;
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

        managementTypeTransformerRegistry.register(new JsonObjectToDataSourceTransformer());
        managementTypeTransformerRegistry.register(new JsonObjectFromDataSourceTransformer(jsonFactory, typeManager, JSON_LD));

        validator.register(DATA_SOURCE_TYPE, DataSourceValidator.instance());

        webService.registerResource(ApiContext.MANAGEMENT, new DataSourceApiV4Controller(dataSourceService, managementTypeTransformerRegistryV4, validator, monitor));
        webService.registerDynamicResource(ApiContext.MANAGEMENT, DataSourceApiV4Controller.class, new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE_V4, validator, ManagementApiJsonSchema.V4.version()));
    }
}
