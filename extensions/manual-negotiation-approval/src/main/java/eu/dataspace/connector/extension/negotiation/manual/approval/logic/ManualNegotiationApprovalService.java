package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import eu.dataspace.connector.extension.negotiation.manual.approval.command.ApproveNegotiationCommand;
import eu.dataspace.connector.extension.negotiation.manual.approval.command.RejectNegotiationCommand;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

public class ManualNegotiationApprovalService {

    private final TransactionContext transactionContext;
    private final CommandHandlerRegistry commandHandlerRegistry;

    public ManualNegotiationApprovalService(TransactionContext transactionContext, CommandHandlerRegistry commandHandlerRegistry) {
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
    }

    /**
     * Approve a pending negotiation.
     *
     * @param negotiationId negotiation id.
     * @return result.
     */
    public ServiceResult<Void> approve(String negotiationId) {
        return transactionContext.execute(() -> commandHandlerRegistry
                .execute(new ApproveNegotiationCommand(negotiationId))
                .flatMap(ServiceResult::from)
        );
    }

    /**
     * Reject a pending negotiation.
     *
     * @param negotiationId negotiation id.
     * @return result.
     */
    public ServiceResult<Void> reject(String negotiationId) {
        return transactionContext.execute(() -> commandHandlerRegistry
                .execute(new RejectNegotiationCommand(negotiationId))
                .flatMap(ServiceResult::from)
        );
    }

}
