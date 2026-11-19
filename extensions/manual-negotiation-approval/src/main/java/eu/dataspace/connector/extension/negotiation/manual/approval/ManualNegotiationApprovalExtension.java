package eu.dataspace.connector.extension.negotiation.manual.approval;

import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualApprovalValidator;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static eu.dataspace.connector.extension.negotiation.manual.approval.ManualNegotiationApprovalExtension.NAME;

/**
 * Holds provider negotiations for manual approval when the contract definition has the {@code manualApproval} private
 * property set to {@code true}, through a pre-validator on the EDC {@code approval.contract.negotiation} policy scope.
 * Approval and rejection are handled by the EDC contract negotiation API.
 * <p>
 * The EDC manual approval must be enabled with {@code edc.negotiation.approval.enabled=true}, otherwise the scope is
 * never evaluated and negotiations are never held.
 */
@Extension(NAME)
public class ManualNegotiationApprovalExtension implements ServiceExtension {

    static final String NAME = "Manual Negotiation Approval";
    static final String APPROVAL_ENABLED_SETTING = "edc.negotiation.approval.enabled";

    @Setting(description = "EDC setting that enables the manual approval of provider contract negotiations, read here only to warn when it is not enabled",
            key = APPROVAL_ENABLED_SETTING, defaultValue = "false")
    private boolean approvalEnabled;

    @Inject
    private PolicyEngine policyEngine;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        policyEngine.registerPreValidator(ApprovalContractNegotiationPolicyContext.class, new ManualApprovalValidator());

        if (!approvalEnabled) {
            monitor.warning("'%s' is not enabled: contract negotiations will not be held for manual approval, even if their contract definition has the 'manualApproval' private property set to 'true'"
                    .formatted(APPROVAL_ENABLED_SETTING));
        }
    }

}
