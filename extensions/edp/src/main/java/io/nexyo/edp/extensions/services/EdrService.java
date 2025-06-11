package io.nexyo.edp.extensions.services;

import io.nexyo.edp.extensions.exceptions.EdpException;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;

import com.apicatalog.jsonld.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;

import static org.eclipse.edc.spi.query.Criterion.criterion;

public class EdrService {

    private final CatalogService catalogService;
    private final ContractNegotiationService contractNegotiationService;
    private final ContractAgreementService contractAgreementService;
    private final TransferProcessService transferProcessService;
    private final EndpointDataReferenceStore edrStore;

    private final Monitor logger ;

    public EdrService(CatalogService catalogService, ContractNegotiationService contractNegotiationService, ContractAgreementService contractAgreementService, TransferProcessService transferProcessService, EndpointDataReferenceStore edrStore, Monitor logger) {
        this.catalogService = catalogService;
        this.contractNegotiationService = contractNegotiationService;
        this.contractAgreementService = contractAgreementService;
        this.transferProcessService = transferProcessService;
        this.edrStore = edrStore;
        this.logger = logger ;
    }

    /**
     * Retrieves Endpoint Data Reference properties from the contract.
     *
     * @param contractId the contract ID.
     * @return the corresponding value of the given key.
     */
    public String getEdrProperty(String contractId, String key) {
        var transferProcess = this.getCurrentTransferProcess(contractId);

        var endpointDataReference = this.edrStore.resolveByTransferProcess(transferProcess.getId());
        if (endpointDataReference.failed()) {
            throw new EdpException("Endpoint Data Reference not found for transfer process. The error messages are: " +
                    String.join("; ", endpointDataReference.getFailureMessages()) );
        }

        var edrProperties = endpointDataReference.getContent()
                .getProperties();
        var edrPropertyValue = edrProperties.getOrDefault(key, "")
                .toString();

        if (StringUtils.isBlank(edrPropertyValue)) {
            throw new EdpException("Could not extract EDR property for key " + key);
        }

        return edrPropertyValue;
    }

    /**
     * Retrieves the current transfer process for a given contract ID.
     *
     * @param contractId the contract ID.
     * @return the current transfer process.
     */
    public TransferProcess getCurrentTransferProcess(String contractId) {
        var contractAgreement = this.contractAgreementService.findById(contractId);
        if (contractAgreement == null) {
            throw new EdpException("Contract agreement not found for contract ID: " + contractId);
        }

        var querySpec = QuerySpec.Builder.newInstance()
                .filter(criterion("contractId", "=", contractId))
                .build();
        var transferProcesses = this.transferProcessService.search(querySpec);

        var currentTransferProcess = transferProcesses.map(it -> it.stream()
                        .filter(tp -> tp.getState() == TransferProcessStates.STARTED.code())
                        .min(Comparator.comparing(TransferProcess::getStateTimestamp))
                        .orElse(null))
                .orElse(null);

        if (currentTransferProcess == null) {
            throw new EdpException("Transfer process not found for contract ID: " + contractId);
        }

        return currentTransferProcess;
    }

    /**
     * Retrieves the contract agreement for a given contract ID.
     * @param contractId the contract ID.
     * @return the contract agreement.
     */
    public ContractAgreement getContractAgreement(String contractId) {
        var contractAgreement = this.contractAgreementService.findById(contractId);

        if (contractAgreement == null) {
            throw new EdpException("Contract agreement not found for contract ID: " + contractId);
        }
        return contractAgreement;
    }
}
