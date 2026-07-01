package eu.dataspace.connector.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * This extension registers a "fake" idsa-context, because the original context url that is added in the claims by the
 * DapsExtension is not reachable anymore.
 * The problem surfaced when the participant claims got stored in the ContractAgreement, and eventually expanded when
 * requested through Management Api.
 *
 * This extension could be eventually removed the day DAPS will be completely replaced by DCP and no idsa context can
 * be found in the ContractAgreement/TransferProcess claims field.
 *
 * So very likely it needs to stay here forever, as agreements/transfer data is usually retained in the connectors.
 *
 * see. <a href="https://github.com/Mobility-Data-Space/mds-edc/issues/493">github issue</a>
 */
public class IdsaContextExtension implements ServiceExtension {

    @Inject
    private JsonLd jsonLd;

    @Override
    public void initialize(ServiceExtensionContext context) {
        try {
            var resource = getClass().getClassLoader().getResource("idsa-context.jsonld");
            Objects.requireNonNull(resource, "idsa-context.jsonld resource not found");
            jsonLd.registerCachedDocument("https://w3id.org/idsa/contexts/context.jsonld", resource.toURI());
        } catch (URISyntaxException e) {
            throw new EdcException("Cannot register idsa fake context, it's mandatory to ensure the connector is working correctly.", e);
        }
    }
}
