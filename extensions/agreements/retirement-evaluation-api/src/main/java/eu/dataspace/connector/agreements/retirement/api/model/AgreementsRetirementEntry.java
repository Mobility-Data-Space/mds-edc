package eu.dataspace.connector.agreements.retirement.api.model;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Legacy representation of a retired {@link ContractAgreement}, kept to serve the deprecated
 * {@code /contractagreements/retirements} endpoints. The retirement state now lives on the {@link ContractAgreement}
 * itself, managed by the EDC {@code ContractAgreementService}.
 *
 * @param agreementId             the contract agreement id.
 * @param reason                  the retirement reason.
 * @param agreementRetirementDate the retirement date, in epoch seconds.
 * @deprecated use the EDC contract agreement API ({@code /v4/contractagreements/{id}/retire}).
 */
@Deprecated(since = "2.9.0")
public record AgreementsRetirementEntry(String agreementId, String reason, long agreementRetirementDate) {

    public static final String AGREEMENTS_RETIREMENT_ENTRY_TYPE = "AgreementsRetirementEntry";
    public static final String AR_ENTRY_TYPE = EDC_NAMESPACE + AGREEMENTS_RETIREMENT_ENTRY_TYPE;

    public static final String AR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String AR_ENTRY_REASON = EDC_NAMESPACE + "reason";
    public static final String AR_ENTRY_RETIREMENT_DATE = EDC_NAMESPACE + "agreementRetirementDate";

    public static AgreementsRetirementEntry from(ContractAgreement agreement) {
        return new AgreementsRetirementEntry(agreement.getId(), agreement.getRetirementReason(), agreement.getRetirementDate());
    }
}
