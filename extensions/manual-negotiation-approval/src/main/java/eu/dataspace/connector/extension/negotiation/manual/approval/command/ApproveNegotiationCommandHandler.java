package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import eu.dataspace.connector.extension.negotiation.manual.approval.event.ContractNegotiationManuallyApproved;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.EntityCommandHandler;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;
import java.util.function.Predicate;

public class ApproveNegotiationCommandHandler extends EntityCommandHandler<ApproveNegotiationCommand, ContractNegotiation> {

    private final EventRouter eventRouter;
    private final Clock clock;
    private final Predicate<ContractNegotiation> validation = CommandValidation.eligibleForManualApprovalRejection;

    public ApproveNegotiationCommandHandler(ContractNegotiationStore store, EventRouter eventRouter, Clock clock) {
        super(store);
        this.eventRouter = eventRouter;
        this.clock = clock;
    }

    @Override
    protected boolean modify(ContractNegotiation entity, ApproveNegotiationCommand command) {
        if (!validation.test(entity)) {
            return false;
        }

        entity.transitionAgreeing();
        entity.setPending(false);
        return true;
    }

    @Override
    public Class<ApproveNegotiationCommand> getType() {
        return ApproveNegotiationCommand.class;
    }

    @Override
    public void postActions(ContractNegotiation entity, ApproveNegotiationCommand command) {
        var event = ContractNegotiationManuallyApproved.Builder.newInstance()
                .contractNegotiationId(entity.getId())
                .counterPartyAddress(entity.getCounterPartyAddress())
                .counterPartyId(entity.getCounterPartyId())
                .protocol(entity.getProtocol())
                .build();

        var envelope = EventEnvelope.Builder.newInstance().at(clock.millis()).payload(event).build();
        eventRouter.publish(envelope);
    }
}
