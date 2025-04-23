package eu.dataspace.connector.extension.negotiation.manual.approval.api;

import eu.dataspace.connector.extension.negotiation.manual.approval.command.ApproveNegotiationCommand;
import eu.dataspace.connector.extension.negotiation.manual.approval.command.RejectNegotiationCommand;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Path("/v3/contractnegotiations")
public class ManualNegotiationApprovalApiController implements ManualNegotiationApprovalApi {

    private final TransactionContext transactionContext;
    private final CommandHandlerRegistry commandHandlerRegistry;

    public ManualNegotiationApprovalApiController(TransactionContext transactionContext, CommandHandlerRegistry commandHandlerRegistry) {
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
    }

    @POST
    @Path("/{id}/approve")
    @Override
    public void approveNegotiation(@PathParam("id") String id) {
        transactionContext.execute(() -> {
            commandHandlerRegistry.execute(new ApproveNegotiationCommand(id));
        });
    }

    @POST
    @Path("/{id}/reject")
    @Override
    public void rejectNegotiation(@PathParam("id") String id) {
        transactionContext.execute(() -> {
            commandHandlerRegistry.execute(new RejectNegotiationCommand(id));
        });
    }
}
