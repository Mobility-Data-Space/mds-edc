package eu.dataspace.connector.extension.negotiation.manual.approval;

import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuardRegistry;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static eu.dataspace.connector.extension.negotiation.manual.approval.ManualNegotiationApprovalExtension.NAME;

/**
 * Holds provider negotiations for manual approval when the contract definition has the {@code manualApproval} private
 * property set to {@code true}. Approval and rejection are handled by the EDC contract negotiation API.
 */
@Extension(NAME)
public class ManualNegotiationApprovalExtension implements ServiceExtension {

    static final String NAME = "Manual Negotiation Approval";

    @Inject
    private ContractNegotiationPendingGuardRegistry pendingGuardRegistry;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        pendingGuardRegistry.register(new ManualNegotiationApprovalPendingGuard(contractDefinitionStore));
    }

}
