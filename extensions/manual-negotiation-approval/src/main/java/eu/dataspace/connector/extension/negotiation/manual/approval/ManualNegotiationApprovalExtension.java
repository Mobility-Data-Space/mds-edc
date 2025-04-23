package eu.dataspace.connector.extension.negotiation.manual.approval;

import eu.dataspace.connector.extension.negotiation.manual.approval.api.ManualNegotiationApprovalApiController;
import eu.dataspace.connector.extension.negotiation.manual.approval.command.ApproveNegotiationCommandHandler;
import eu.dataspace.connector.extension.negotiation.manual.approval.command.RejectNegotiationCommandHandler;
import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;

public class ManualNegotiationApprovalExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CommandHandlerRegistry commandHandlerRegistry;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        commandHandlerRegistry.register(new ApproveNegotiationCommandHandler(contractNegotiationStore));
        commandHandlerRegistry.register(new RejectNegotiationCommandHandler(contractNegotiationStore));

        webService.registerResource(MANAGEMENT, new ManualNegotiationApprovalApiController(transactionContext, commandHandlerRegistry));
    }

    @Provider
    public ContractNegotiationPendingGuard contractNegotiationPendingGuard() {
        return new ManualNegotiationApprovalPendingGuard(contractDefinitionStore);
    }

}
