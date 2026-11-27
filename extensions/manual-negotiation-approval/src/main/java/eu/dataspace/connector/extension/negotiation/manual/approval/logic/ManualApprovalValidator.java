package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Pre-validator registered on the {@link ApprovalContractNegotiationPolicyContext#APPROVAL_SCOPE}: it fails the evaluation
 * when the contract definition that offered the asset has the {@code manualApproval} private property set to {@code true},
 * so that the EDC approval guard holds the provider negotiation in {@code REQUESTED} state until it gets manually
 * approved or rejected.
 *
 * @deprecated the {@code manualApproval} contract definition private property is going to be removed, a
 *         {@code ManualApproval} constraint on the contract policy should be used instead, see {@link ManualApprovalPolicyFunction}.
 */
@Deprecated(since = "1.0.0")
public class ManualApprovalValidator implements PolicyValidatorRule<ApprovalContractNegotiationPolicyContext> {

    static final String MANUAL_APPROVAL_PROPERTY = EDC_NAMESPACE + "manualApproval";

    private final Monitor monitor;

    public ManualApprovalValidator(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Boolean apply(Policy policy, ApprovalContractNegotiationPolicyContext context) {
        var contractDefinition = context.contractDefinition();
        if (contractDefinition != null && Objects.equals(contractDefinition.getPrivateProperty(MANUAL_APPROVAL_PROPERTY), "true")) {
            monitor.warning("Contract definition %s uses the deprecated 'manualApproval' private property, that will be removed: use a 'ManualApproval' constraint on the contract policy instead"
                    .formatted(contractDefinition.getId()));
            context.reportProblem("Contract definition %s requires manual approval".formatted(contractDefinition.getId()));
            return false;
        }
        return true;
    }
}
