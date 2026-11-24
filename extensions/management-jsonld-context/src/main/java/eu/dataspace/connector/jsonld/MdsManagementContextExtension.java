package eu.dataspace.connector.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import static eu.dataspace.connector.jsonld.MdsManagementContextExtension.NAME;
import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE_V4;

@Extension(value = NAME)
public class MdsManagementContextExtension implements ServiceExtension {

    public static final String NAME = "MDS Management Context Extension";
    public static final String MDS_MANAGEMENT_CONTEXT_URL = "https://w3id.org/mobility-dataspace/connector/management/v1";
    static final String CONTEXT_FILE = "document/mds-management-context-v1.jsonld";
    public static final String MDS_POLICY_CONTEXT_URL = "https://w3id.org/mobility-dataspace/2026/1/policy/context.jsonld";
    static final String MDS_POLICY_CONTEXT_FILE = "document/mds-policy-context-2026-1.jsonld";

    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLd.registerCachedDocument(MDS_MANAGEMENT_CONTEXT_URL, resource(CONTEXT_FILE));
        jsonLd.registerContext(MDS_MANAGEMENT_CONTEXT_URL, MANAGEMENT_SCOPE_V4);
        jsonLd.registerCachedDocument(MDS_POLICY_CONTEXT_URL, resource(MDS_POLICY_CONTEXT_FILE));
    }

    private URI resource(String file) {
        try {
            var resource = getClass().getClassLoader().getResource(file);
            Objects.requireNonNull(resource, file + " resource not found");
            return resource.toURI();
        } catch (URISyntaxException e) {
            throw new EdcException("Cannot load JSON-LD document " + file, e);
        }
    }
}
