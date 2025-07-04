package eu.dataspace.connector.extension.contract.retirement.event;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

public abstract class ContractAgreementEvent extends Event {

    protected String contractAgreementId;

    public String getContractAgreementId() {
        return contractAgreementId;
    }

    public abstract static class Builder<T extends ContractAgreementEvent, B extends Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B contractAgreementId(String contractAgreementId) {
            event.contractAgreementId = contractAgreementId;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.contractAgreementId);

            return event;
        }
    }
}
