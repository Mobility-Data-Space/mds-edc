package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.CommandFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.command.CommandFailure.Reason.CONFLICT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApproveNegotiationCommandHandlerTest {

    private final ContractNegotiationStore store = mock();
    private final ApproveNegotiationCommandHandler handler = new ApproveNegotiationCommandHandler(store, mock(), mock());

    @Test
    void shouldFail_whenTypeIsConsumer() {
        var id = UUID.randomUUID().toString();
        var contractNegotiation = contractNegotiationBuilder().type(CONSUMER).state(REQUESTED.code()).build();
        when(store.findByIdAndLease(id)).thenReturn(StoreResult.success(contractNegotiation));

        var commandResult = handler.handle(new ApproveNegotiationCommand(id));

        assertThat(commandResult).isFailed().extracting(CommandFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    void shouldFail_whenTypeProviderButNotPending() {
        var id = UUID.randomUUID().toString();
        var contractNegotiation = contractNegotiationBuilder().type(PROVIDER).state(REQUESTED.code()).pending(false).build();
        when(store.findByIdAndLease(id)).thenReturn(StoreResult.success(contractNegotiation));

        var commandResult = handler.handle(new ApproveNegotiationCommand(id));

        assertThat(commandResult).isFailed().extracting(CommandFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    void shouldFail_whenTypeProviderButStateNotRequested() {
        var id = UUID.randomUUID().toString();
        var contractNegotiation = contractNegotiationBuilder().type(PROVIDER).state(AGREED.code()).pending(true).build();
        when(store.findByIdAndLease(id)).thenReturn(StoreResult.success(contractNegotiation));

        var commandResult = handler.handle(new ApproveNegotiationCommand(id));

        assertThat(commandResult).isFailed().extracting(CommandFailure::getReason).isEqualTo(CONFLICT);
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .counterPartyId("any")
                .counterPartyAddress("any")
                .protocol("any");
    }
}
