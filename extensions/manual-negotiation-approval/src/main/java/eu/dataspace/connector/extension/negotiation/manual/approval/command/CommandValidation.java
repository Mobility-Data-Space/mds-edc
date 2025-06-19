package eu.dataspace.connector.extension.negotiation.manual.approval.command;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.function.Predicate;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

public interface CommandValidation {

    Predicate<ContractNegotiation> isProvider = contractNegotiation -> contractNegotiation.getType() == ContractNegotiation.Type.PROVIDER;
    Predicate<ContractNegotiation> isPending = StatefulEntity::isPending;
    Predicate<ContractNegotiation> isRequested = contractNegotiation -> contractNegotiation.getState() == REQUESTED.code();

    Predicate<ContractNegotiation> eligibleForManualApprovalRejection = isProvider.and(isPending).and(isRequested);

}
