package eu.dataspace.dataplane.dfrs.model;

import eu.dataspace.connector.agreements.retirement.spi.event.ContractAgreementRetired;

public record ObserverContractAgreementRetired(
        String contractAgreementId
) implements ObserverEvent {

    public static ObserverContractAgreementRetired from(ContractAgreementRetired event) {
        return new ObserverContractAgreementRetired(event.getContractAgreementId());
    }

    @Override
    public String eventType() {
        return "eu.dataspace.mds.ContractAgreementRetired";
    }
}
