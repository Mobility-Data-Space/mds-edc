package eu.dataspace.dataplane.dfrs.model;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;

import java.time.Clock;
import java.util.UUID;

public record ObserverEventEnvelope(
        String specversion,
        String id,
        String source,
        String type,
        String time,
        String datacontenttype,
        Object data
) {

    private static final String SPEC_VERSION = "1.0";
    private static final String APPLICATION_JSON = "application/json";

    public static ObserverEventEnvelope create(ContractNegotiationFinalized finalized, ParticipantContext participantContext, Clock clock) {
        var contractAgreement = finalized.getContractAgreement();

        var observerContractAgreement = new ObserverContractAgreement(
                contractAgreement.getId(),
                contractAgreement.getProviderId(),
                contractAgreement.getConsumerId(),
                contractAgreement.getAssetId(),
                contractAgreement.getContractSigningDate(),
                contractAgreement.getPolicy()
        );

        var observerEvent = new ObserverContractNegotiationFinalized(
                finalized.getContractNegotiationId(),
                finalized.getCounterPartyAddress(),
                finalized.getCounterPartyId(),
                finalized.getProtocol(),
                observerContractAgreement
        );

        return new ObserverEventEnvelope(
                SPEC_VERSION,
                UUID.randomUUID().toString(),
                participantContext.getIdentity(),
                "org.eclipse.edc.ContractNegotiationFinalized",
                clock.instant().toString(),
                APPLICATION_JSON,
                observerEvent
        );
    }
}
