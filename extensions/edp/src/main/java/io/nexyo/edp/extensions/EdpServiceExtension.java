package io.nexyo.edp.extensions;

import io.nexyo.edp.extensions.controllers.DaseenController;
import io.nexyo.edp.extensions.controllers.EdpsController;
import io.nexyo.edp.extensions.exceptions.EdpExceptionMapper;
import io.nexyo.edp.extensions.services.DaseenService;
import io.nexyo.edp.extensions.services.DataplaneService;
import io.nexyo.edp.extensions.services.EdpsService;
import io.nexyo.edp.extensions.services.EdrService;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

/**
 * The EdpServiceExtension class is responsible for initializing the EDP service
 * extension.
 */
public class EdpServiceExtension implements ServiceExtension {

    public static final String EXTENSION_NAME = "EdpServiceExtension";

    @Setting(key = "edp.dataplane.callback.url")
    private String dataplaneCallbackUrl;

    @Setting(key = "edp.daseen.api.key")
    private String daseenApiKey;

    @Inject
    private WebService webService;

    @Inject
    private DataPlaneClientFactory clientFactory;

    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;

    @Inject
    private AssetService assetService;

    @Inject
    private CatalogService catalogService;

    @Inject
    private ContractAgreementService contractAgreementService;

    @Inject
    private ContractNegotiationService contractNegotiationService;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private EndpointDataReferenceStore edrStore;

    @Inject
    private AssetIndex assetIndexer;

    private EdpsService edpsService;

    private DaseenService daseenService;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var dataplaneService = new DataplaneService(dataPlaneSelectorService, clientFactory, assetIndexer, dataplaneCallbackUrl, monitor);
        var edrService = new EdrService(catalogService, contractNegotiationService, contractAgreementService, transferProcessService, edrStore, monitor);
        edpsService = new EdpsService(dataplaneService, edrService, monitor);
        daseenService = new DaseenService(dataplaneService, edrService, monitor, daseenApiKey);
        var edpsController = new EdpsController(edpsService, assetService, monitor);
        var daseenController = new DaseenController(daseenService, assetService, monitor);

        webService.registerResource(ApiContext.MANAGEMENT, edpsController);
        webService.registerResource(ApiContext.MANAGEMENT, daseenController);
        webService.registerResource(ApiContext.MANAGEMENT, new EdpExceptionMapper());
    }

    @Override
    public void shutdown() {
        edpsService.close();
        daseenService.close();
    }

}
