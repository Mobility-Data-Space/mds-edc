package io.nexyo.edp.extensions.services;

import io.nexyo.edp.extensions.exceptions.EdpException;
import io.nexyo.edp.extensions.model.Edr;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.constants.CoreConstants;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Comparator;

import static org.eclipse.edc.spi.query.Criterion.criterion;

public class EdrService {

    private final ContractAgreementService contractAgreementService;
    private final TransferProcessService transferProcessService;
    private final EndpointDataReferenceStore edrStore;

    public EdrService(ContractAgreementService contractAgreementService, TransferProcessService transferProcessService, EndpointDataReferenceStore edrStore) {
        this.contractAgreementService = contractAgreementService;
        this.transferProcessService = transferProcessService;
        this.edrStore = edrStore;
    }

    /**
     * Retrieves EDR for the contract
     *
     * @param contractId the contract id
     * @return the EDR if found, failure otherwise.
     */
    public ServiceResult<Edr> getEdr(String contractId) {
        var transferProcess = getCurrentTransferProcess(contractId);

        return edrStore.resolveByTransferProcess(transferProcess.getId())
                .flatMap(ServiceResult::from)
                .map(endpointDataReference -> new Edr(
                        endpointDataReference.getStringProperty(CoreConstants.EDC_NAMESPACE + "endpoint"),
                        endpointDataReference.getStringProperty(CoreConstants.EDC_NAMESPACE + "authorization")
                ));
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
