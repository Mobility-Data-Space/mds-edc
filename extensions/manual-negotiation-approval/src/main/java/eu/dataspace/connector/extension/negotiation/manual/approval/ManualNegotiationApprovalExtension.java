package eu.dataspace.connector.extension.negotiation.manual.approval;

import eu.dataspace.connector.extension.negotiation.manual.approval.api.ManualNegotiationApprovalApiController;
import eu.dataspace.connector.extension.negotiation.manual.approval.command.ApproveNegotiationCommandHandler;
import eu.dataspace.connector.extension.negotiation.manual.approval.command.RejectNegotiationCommandHandler;
import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalPendingGuard;
import eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalService;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;

import java.time.Clock;

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
    @Inject
    private EventRouter eventRouter;
    @Inject
    private Clock clock;

    @Override
    public void initialize(ServiceExtensionContext context) {
        commandHandlerRegistry.register(new ApproveNegotiationCommandHandler(contractNegotiationStore, eventRouter, clock));
        commandHandlerRegistry.register(new RejectNegotiationCommandHandler(contractNegotiationStore, eventRouter, clock));

        var service = new ManualNegotiationApprovalService(transactionContext, commandHandlerRegistry);
        webService.registerResource(MANAGEMENT, new ManualNegotiationApprovalApiController(service));
    }

    @Provider
    public ContractNegotiationPendingGuard contractNegotiationPendingGuard() {
        return new ManualNegotiationApprovalPendingGuard(contractDefinitionStore);
    }

}
