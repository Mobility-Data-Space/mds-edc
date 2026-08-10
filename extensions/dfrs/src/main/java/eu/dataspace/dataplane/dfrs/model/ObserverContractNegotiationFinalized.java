package eu.dataspace.dataplane.dfrs.model;

public record ObserverContractNegotiationFinalized(
        String contractNegotiationId,
        String counterPartyAddress,
        String counterPartyId,
        String protocol,
        ObserverContractAgreement contractAgreement
) {
}
