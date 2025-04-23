package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.EntityCommandHandler;

public class RejectNegotiationCommandHandler extends EntityCommandHandler<RejectNegotiationCommand, ContractNegotiation> {

    public RejectNegotiationCommandHandler(ContractNegotiationStore store) {
        super(store);
    }

    @Override
    protected boolean modify(ContractNegotiation entity, RejectNegotiationCommand command) {
        entity.transitionTerminating("Negotiation manually rejected");
        entity.setPending(false);
        return true;
    }

    @Override
    public Class<RejectNegotiationCommand> getType() {
        return RejectNegotiationCommand.class;
    }
}
