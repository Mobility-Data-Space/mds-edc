package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.EntityCommandHandler;

public class ApproveNegotiationCommandHandler extends EntityCommandHandler<ApproveNegotiationCommand, ContractNegotiation> {

    public ApproveNegotiationCommandHandler(ContractNegotiationStore store) {
        super(store);
    }

    @Override
    protected boolean modify(ContractNegotiation entity, ApproveNegotiationCommand command) {
        entity.transitionAgreeing();
        entity.setPending(false);
        return true;
    }

    @Override
    public Class<ApproveNegotiationCommand> getType() {
        return ApproveNegotiationCommand.class;
    }
}
