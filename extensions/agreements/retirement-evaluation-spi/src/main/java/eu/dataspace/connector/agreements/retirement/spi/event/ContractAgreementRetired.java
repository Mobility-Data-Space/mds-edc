package eu.dataspace.connector.agreements.retirement.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ContractAgreementRetired.Builder.class)
public class ContractAgreementRetired extends ContractAgreementEvent {

    private ContractAgreementRetired() {
    }

    @Override
    public String name() {
        return "contract.agreement.retired";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractAgreementEvent.Builder<ContractAgreementRetired, Builder> {

        private Builder() {
            super(new ContractAgreementRetired());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
