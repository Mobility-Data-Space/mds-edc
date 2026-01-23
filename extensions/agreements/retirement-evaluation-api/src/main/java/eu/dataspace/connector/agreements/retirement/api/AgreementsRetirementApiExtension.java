package eu.dataspace.connector.agreements.retirement.api;

import eu.dataspace.connector.agreements.retirement.api.transform.JsonObjectFromAgreementRetirementTransformer;
import eu.dataspace.connector.agreements.retirement.api.transform.JsonObjectToAgreementsRetirementEntryTransformer;
import eu.dataspace.connector.agreements.retirement.api.v3.AgreementsRetirementApiV3Controller;
import eu.dataspace.connector.agreements.retirement.spi.service.AgreementsRetirementService;
import jakarta.json.Json;
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

import static eu.dataspace.connector.agreements.retirement.api.AgreementsRetirementApiExtension.NAME;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;


@Extension(value = NAME)
public class AgreementsRetirementApiExtension implements ServiceExtension {

    public static final String NAME = "Contract Agreement Retirement API ";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonObjectValidatorRegistry validator;
    @Inject
    private AgreementsRetirementService agreementsRetirementService;
    @Inject
    private Monitor monitor;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        managementTypeTransformerRegistry.register(new JsonObjectFromAgreementRetirementTransformer(jsonFactory));
        managementTypeTransformerRegistry.register(new JsonObjectToAgreementsRetirementEntryTransformer(monitor));

        webService.registerResource(ApiContext.MANAGEMENT, new AgreementsRetirementApiV3Controller(agreementsRetirementService, managementTypeTransformerRegistry, validator, monitor));
        var jsonLdInterceptor = new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, MANAGEMENT_SCOPE);
        webService.registerDynamicResource(ApiContext.MANAGEMENT, AgreementsRetirementApiV3Controller.class, jsonLdInterceptor);
    }

}
