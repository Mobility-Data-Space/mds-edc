package io.nexyo.edp.extensions.services;

import io.nexyo.edp.extensions.exceptions.EdpException;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;

import java.net.URI;
import java.util.UUID;

/**
 * Service class responsible for managing data plane interactions, including
 * selecting data plane instances and initiating data transfers.
 */
public class DataplaneService {

    private DataPlaneClientFactory clientFactory;
    private DataPlaneSelectorService selectorService;
    private AssetIndex assetIndexer;
    private Monitor logger;
    private String callbackAddress;

    /**
     * Default constructor for DataplaneService.
     */
    private DataplaneService() {
    }

    /**
     * Constructs a new DataplaneService with the given dependencies.
     *
     * @param dataPlaneSelectorService the service for selecting data plane
     *                                 instances.
     * @param clientFactory            the factory for creating data plane clients.
     * @param assetIndexer             the indexer for resolving asset addresses.
     * @param callbackAddress          the callback address
     * @param monitor                  the monitor
     */
    public DataplaneService(DataPlaneSelectorService dataPlaneSelectorService, DataPlaneClientFactory clientFactory,
                            AssetIndex assetIndexer, String callbackAddress, Monitor monitor) {
        this.selectorService = dataPlaneSelectorService;
        this.clientFactory = clientFactory;
        this.assetIndexer = assetIndexer;
        this.logger = monitor;
        this.callbackAddress = callbackAddress;
    }

    /**
     * Selects an appropriate data plane instance for a given data address.
     *
     * @param dataAddress the data address for which to select a data plane.
     * @return the selected data plane instance.
     * @throws EdpException if no suitable data plane instance is found.
     */
    private DataPlaneInstance getDataplane(DataAddress dataAddress) {
        var selection = selectorService.select(dataAddress, "HttpData-PUSH", null);
        var dataPlaneInstance = selection.getContent();

        if (dataPlaneInstance == null) {
            throw new EdpException("No data plane instance found");
        }
        return dataPlaneInstance;
    }

    /**
     * Starts a data transfer for a given asset ID to a destination address.
     *
     * @param assetId            the ID of the asset to transfer.
     * @param destinationAddress the destination data address.
     * @throws EdpException if the source address for the asset is not found.
     */
    public void start(String assetId, DataAddress destinationAddress, String processId, String participantId,
            String agreementId) {
        var sourceAddress = this.assetIndexer.resolveForAsset(assetId);

        if (sourceAddress == null) {
            this.logger.severe("No source address found for asset id: " + assetId);
            throw new EdpException("No source address found for asset id: " + assetId);
        }

        var dataplaneInstance = getDataplane(sourceAddress);
        this.logger.info("Data flow starting with dataplane id: " + dataplaneInstance.getId());

        var dataFlowRequest = createDataFlowRequest(assetId, sourceAddress, destinationAddress, processId,
                participantId, agreementId);

        var result = clientFactory.createClient(dataplaneInstance)
                .start(dataFlowRequest)
                .map(it -> DataFlowResponse.Builder.newInstance()
                        .dataAddress(it.getDataAddress())
                        .dataPlaneId(dataplaneInstance.getId())
                        .build());
        this.logger.info("Data flow response is: " + result);
    }

    /**
     * Starts a data transfer between two data addresses.
     *
     * @param sourceAddress      the source data address.
     * @param destinationAddress the destination data address.
     */
    public void start(DataAddress sourceAddress, DataAddress destinationAddress, String processId, String participantId,
            String agreementId) {
        var dataplaneInstance = getDataplane(sourceAddress);
        this.logger.info("Data flow starting with dataplane id: " + dataplaneInstance.getId());

        var dataFlowRequest = createDataFlowRequest(null, sourceAddress, destinationAddress, processId, participantId,
                agreementId);

        var result = clientFactory.createClient(dataplaneInstance)
                .start(dataFlowRequest)
                .map(it -> DataFlowResponse.Builder.newInstance()
                        .dataAddress(it.getDataAddress())
                        .dataPlaneId(dataplaneInstance.getId())
                        .build());
        this.logger.info("Data flow response is: " + result);
    }

    /**
     * Creates a data flow request for transferring data.
     *
     * @param assetId                the ID of the asset being transferred
     *                               (optional).
     * @param sourceDataAddress      the source data address.
     * @param destinationDataAddress the destination data address.
     * @return a {@link DataFlowStartMessage} representing the request.
     */
    private DataFlowStartMessage createDataFlowRequest(String assetId, DataAddress sourceDataAddress,
            DataAddress destinationDataAddress, String processId, String participantId, String agreementId) {
        TransferType transferType = new TransferType("HttpData", FlowType.PUSH);

        return DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(assetId)
                .sourceDataAddress(sourceDataAddress)
                .destinationDataAddress(destinationDataAddress)
                .processId(processId)
                .participantId(participantId)
                .agreementId(agreementId)
                .callbackAddress(URI.create(this.callbackAddress))
                .transferType(transferType)
                .build();
    }
}
