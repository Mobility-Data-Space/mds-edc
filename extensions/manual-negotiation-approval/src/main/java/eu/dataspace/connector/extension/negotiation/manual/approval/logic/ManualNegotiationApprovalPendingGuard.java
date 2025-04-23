package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;

import java.util.Objects;
import java.util.Optional;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class ManualNegotiationApprovalPendingGuard implements ContractNegotiationPendingGuard {
    private final ContractDefinitionStore contractDefinitionStore;

    public ManualNegotiationApprovalPendingGuard(ContractDefinitionStore contractDefinitionStore) {
        this.contractDefinitionStore = contractDefinitionStore;
    }

    @Override
    public boolean test(ContractNegotiation contractNegotiation) {
        if (contractNegotiation.getType() == ContractNegotiation.Type.PROVIDER && contractNegotiation.getState() == REQUESTED.code()) {
            var result = ContractOfferId.parseId(contractNegotiation.getLastContractOffer().getId())
                    .map(ContractOfferId::definitionPart)
                    .map(contractDefinitionStore::findById)
                    .map(Optional::ofNullable)
                    .map(opt -> opt
                            .map(it -> it.getPrivateProperty(EDC_NAMESPACE + "manualApproval"))
                            .map(it -> Objects.equals(it, "true"))
                            .orElse(false)
                    );
            
            if (result.succeeded()) {
                return result.getContent();
            } else {
                return false;
            }
        }

        return false;
    }
}
