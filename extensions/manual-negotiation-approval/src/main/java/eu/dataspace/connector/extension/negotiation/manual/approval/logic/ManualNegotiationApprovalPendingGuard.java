package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;

import java.util.Objects;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Holds provider negotiations in {@code REQUESTED} state whose contract definition has the {@code manualApproval} private
 * property set to {@code true}.
 */
public class ManualNegotiationApprovalPendingGuard implements ContractNegotiationPendingGuard {

    static final String MANUAL_APPROVAL_PROPERTY = EDC_NAMESPACE + "manualApproval";

    private final ContractDefinitionStore contractDefinitionStore;

    public ManualNegotiationApprovalPendingGuard(ContractDefinitionStore contractDefinitionStore) {
        this.contractDefinitionStore = contractDefinitionStore;
    }

    @Override
    public boolean test(ContractNegotiation contractNegotiation) {
        if (contractNegotiation.getType() != ContractNegotiation.Type.PROVIDER || contractNegotiation.getState() != REQUESTED.code()) {
            return false;
        }

        var offer = contractNegotiation.getLastContractOffer();
        if (offer == null) {
            return false;
        }

        return ContractOfferId.parseId(offer.getId())
                .map(offerId -> contractDefinitionStore.findById(contractNegotiation.getParticipantContextId(), offerId.definitionPart()))
                .map(definition -> definition != null && Objects.equals(definition.getPrivateProperty(MANUAL_APPROVAL_PROPERTY), "true"))
                .orElse(failure -> false);
    }
}
