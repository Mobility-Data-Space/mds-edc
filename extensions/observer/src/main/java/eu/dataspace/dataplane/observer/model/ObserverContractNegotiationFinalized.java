package eu.dataspace.dataplane.observer.model;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;

public record ObserverContractNegotiationFinalized(
        String contractNegotiationId,
        String counterPartyAddress,
        String counterPartyId,
        String protocol,
        ObserverContractAgreement contractAgreement
) implements ObserverEvent {

    public static ObserverContractNegotiationFinalized from(ContractNegotiationFinalized event) {
        var contractAgreement = event.getContractAgreement();

        var observerContractAgreement = new ObserverContractAgreement(
                contractAgreement.getId(),
                contractAgreement.getProviderId(),
                contractAgreement.getConsumerId(),
                contractAgreement.getAssetId(),
                contractAgreement.getContractSigningDate(),
                contractAgreement.getPolicy()
        );

        return new ObserverContractNegotiationFinalized(
                event.getContractNegotiationId(),
                event.getCounterPartyAddress(),
                event.getCounterPartyId(),
                event.getProtocol(),
                observerContractAgreement
        );
    }

    @Override
    public String eventType() {
        return "org.eclipse.edc.ContractNegotiationFinalized";
    }
}
