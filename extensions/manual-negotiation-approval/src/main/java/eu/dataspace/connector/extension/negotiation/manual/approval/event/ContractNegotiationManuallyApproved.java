package eu.dataspace.connector.extension.negotiation.manual.approval.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;

@JsonDeserialize(builder = ContractNegotiationManuallyApproved.Builder.class)
public class ContractNegotiationManuallyApproved extends ContractNegotiationEvent {

    private ContractNegotiationManuallyApproved() {

    }

    @Override
    public String name() {
        return "contract.negotiation.manually.approved";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationManuallyApproved, Builder> {

        @JsonCreator
        private Builder() {
            super(new ContractNegotiationManuallyApproved());
        }

        public static Builder newInstance() {
            return new ContractNegotiationManuallyApproved.Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

    }
}
