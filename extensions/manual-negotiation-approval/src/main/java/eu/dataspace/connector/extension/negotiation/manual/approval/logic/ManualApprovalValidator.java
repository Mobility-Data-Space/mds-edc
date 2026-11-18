package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;

import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Pre-validator registered on the {@link ApprovalContractNegotiationPolicyContext#APPROVAL_SCOPE}: it fails the evaluation
 * when the contract definition that offered the asset has the {@code manualApproval} private property set to {@code true},
 * so that the EDC approval guard holds the provider negotiation in {@code REQUESTED} state until it gets manually
 * approved or rejected.
 */
public class ManualApprovalValidator implements PolicyValidatorRule<ApprovalContractNegotiationPolicyContext> {

    static final String MANUAL_APPROVAL_PROPERTY = EDC_NAMESPACE + "manualApproval";

    @Override
    public Boolean apply(Policy policy, ApprovalContractNegotiationPolicyContext context) {
        var contractDefinition = context.contractDefinition();
        if (contractDefinition != null && Objects.equals(contractDefinition.getPrivateProperty(MANUAL_APPROVAL_PROPERTY), "true")) {
            context.reportProblem("Contract definition %s requires manual approval".formatted(contractDefinition.getId()));
            return false;
        }
        return true;
    }
}
