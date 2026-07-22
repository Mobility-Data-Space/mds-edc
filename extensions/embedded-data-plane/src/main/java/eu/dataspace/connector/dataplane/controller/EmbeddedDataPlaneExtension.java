package eu.dataspace.connector.dataplane.controller;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Provides components to permit embedded communication between control-plane and data-plane
 */
public class EmbeddedDataPlaneExtension implements ServiceExtension {

    @Provider
    public TransferProcessApiClient transferProcessApiClient(ServiceExtensionContext context) {
        return new EmbeddedTransferProcessApiClient(() -> context.getService(TransferProcessService.class));
    }

}
