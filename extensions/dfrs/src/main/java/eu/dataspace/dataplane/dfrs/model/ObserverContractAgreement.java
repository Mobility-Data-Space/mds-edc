package eu.dataspace.dataplane.dfrs.model;

public record ObserverContractAgreement(
        String id,
        String providerId,
        String consumerId,
        String assetId,
        long contractSigningDate,
        Object policy
) {
}
