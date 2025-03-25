package io.nexyo.edp.extensions;

import io.nexyo.edp.extensions.controllers.DaseenController;
import io.nexyo.edp.extensions.controllers.EdpsController;
import io.nexyo.edp.extensions.exceptions.EdpExceptionMapper;
import io.nexyo.edp.extensions.utils.ConfigurationUtils;
import io.nexyo.edp.extensions.utils.LoggingUtils;
import io.nexyo.edp.extensions.services.EdpsService;
import io.nexyo.edp.extensions.services.DaseenService;
import io.nexyo.edp.extensions.services.DataplaneService;
import io.nexyo.edp.extensions.services.EdrService;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
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

    @Inject
    private WebService webService;

    private Monitor logger;

    @Inject
    private DataPlaneClientFactory clientFactory;

    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;

    @Inject
    private AssetService assetService;

    @Inject
    private ContractAgreementService contractAgreementService;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private EndpointDataReferenceStore edrStore;

    @Inject
    AssetIndex assetIndexer;

    private EdpsService edpsService;

    private EdrService edrService;

    private DaseenService daseenService;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        logger = context.getMonitor();
        LoggingUtils.setLogger(logger);
        ConfigurationUtils.loadConfig();
        logger.info("EdpServiceExtension initialized");

        final var dataplaneService = new DataplaneService(dataPlaneSelectorService, clientFactory, assetIndexer);
        this.edrService = new EdrService(contractAgreementService, transferProcessService, edrStore);
        this.edpsService = new EdpsService(dataplaneService, edrService);
        this.daseenService = new DaseenService(dataplaneService, edrService);
        final var edpsController = new EdpsController(edpsService, assetService);
        final var daseenController = new DaseenController(daseenService, assetService);

        webService.registerResource(ApiContext.MANAGEMENT, edpsController);
        webService.registerResource(ApiContext.MANAGEMENT, daseenController);
        webService.registerResource(ApiContext.MANAGEMENT, new EdpExceptionMapper());
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down EDP extension");
        this.edpsService.close();
        this.daseenService.close();
    }

}
