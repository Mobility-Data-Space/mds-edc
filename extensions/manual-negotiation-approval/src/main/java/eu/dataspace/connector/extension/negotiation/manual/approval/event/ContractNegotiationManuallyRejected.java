package eu.dataspace.connector.extension.negotiation.manual.approval.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;

@JsonDeserialize(builder = ContractNegotiationManuallyRejected.Builder.class)
public class ContractNegotiationManuallyRejected extends ContractNegotiationEvent {

    private ContractNegotiationManuallyRejected() {

    }

    @Override
    public String name() {
        return "contract.negotiation.manually.approved";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractNegotiationEvent.Builder<ContractNegotiationManuallyRejected, Builder> {

        @JsonCreator
        private Builder() {
            super(new ContractNegotiationManuallyRejected());
        }

        public static Builder newInstance() {
            return new ContractNegotiationManuallyRejected.Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

    }
}
