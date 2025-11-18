package eu.dataspace.connector.agreements.retirement.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ContractAgreementReactivated.Builder.class)
public class ContractAgreementReactivated extends ContractAgreementEvent {

    private ContractAgreementReactivated() {
    }

    @Override
    public String name() {
        return "contract.agreement.reactivated";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractAgreementEvent.Builder<ContractAgreementReactivated, Builder> {

        private Builder() {
            super(new ContractAgreementReactivated());
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
