package eu.dataspace.connector.agreements.retirement.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import eu.dataspace.connector.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.edc.spi.entity.Entity;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Representation of a Contract Agreement Retirement entry, to be stored in the {@link AgreementsRetirementStore}.
 */
public class AgreementsRetirementEntry extends Entity {

    public static final String AR_ENTRY_TYPE = EDC_NAMESPACE + "AgreementsRetirementEntry";

    public static final String AR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String AR_ENTRY_REASON = EDC_NAMESPACE + "reason";
    public static final String AR_ENTRY_RETIREMENT_DATE = EDC_NAMESPACE + "agreementRetirementDate";
    @Deprecated(since = "1.0.0")
    private static final String TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
    @Deprecated(since = "1.0.0")
    public static final String DEPRECATED_AR_ENTRY_REASON = TX_NAMESPACE + "reason";
    @Deprecated(since = "1.0.0")
    public static final String DEPRECATED_AR_ENTRY_RETIREMENT_DATE = TX_NAMESPACE + "agreementRetirementDate";

    private String agreementId;
    private String reason;
    private long agreementRetirementDate = 0L;

    public AgreementsRetirementEntry() {}

    public String getAgreementId() {
        return agreementId;
    }

    public String getReason() {
        return reason;
    }

    public long getAgreementRetirementDate() {
        return agreementRetirementDate;
    }

    public static class Builder extends Entity.Builder<AgreementsRetirementEntry, AgreementsRetirementEntry.Builder> {

        private Builder() {
            super(new AgreementsRetirementEntry());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder withAgreementId(String agreementId) {
            this.entity.agreementId = agreementId;
            return this;
        }

        public Builder withReason(String reason) {
            this.entity.reason = reason;
            return this;
        }

        public Builder withAgreementRetirementDate(long agreementRetirementDate) {
            this.entity.agreementRetirementDate = agreementRetirementDate;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public AgreementsRetirementEntry build() {
            super.build();
            requireNonNull(entity.agreementId, AR_ENTRY_AGREEMENT_ID);
            requireNonNull(entity.reason, DEPRECATED_AR_ENTRY_REASON);

            if (entity.agreementRetirementDate == 0L) {
                entity.agreementRetirementDate = this.entity.clock.instant().getEpochSecond();
            }

            return entity;
        }
    }
}
