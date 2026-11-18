package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualNegotiationApprovalPendingGuard.MANUAL_APPROVAL_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ManualNegotiationApprovalPendingGuardTest {

    private final ContractDefinitionStore contractDefinitionStore = mock();
    private final ManualNegotiationApprovalPendingGuard guard = new ManualNegotiationApprovalPendingGuard(contractDefinitionStore);

    @Test
    void shouldHold_whenContractDefinitionHasManualApprovalProperty() {
        when(contractDefinitionStore.findById("participantContextId", "definitionId"))
                .thenReturn(contractDefinition(Map.of(MANUAL_APPROVAL_PROPERTY, "true")));

        var result = guard.test(negotiation(PROVIDER, REQUESTED.code()));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotHold_whenContractDefinitionHasNoManualApprovalProperty() {
        when(contractDefinitionStore.findById("participantContextId", "definitionId"))
                .thenReturn(contractDefinition(Map.of()));

        var result = guard.test(negotiation(PROVIDER, REQUESTED.code()));

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHold_whenContractDefinitionDoesNotExist() {
        when(contractDefinitionStore.findById(any(), any())).thenReturn(null);

        var result = guard.test(negotiation(PROVIDER, REQUESTED.code()));

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotHold_whenConsumer() {
        var result = guard.test(negotiation(CONSUMER, REQUESTED.code()));

        assertThat(result).isFalse();
        verifyNoInteractions(contractDefinitionStore);
    }

    @Test
    void shouldNotHold_whenNotRequested() {
        var result = guard.test(negotiation(PROVIDER, AGREED.code()));

        assertThat(result).isFalse();
        verifyNoInteractions(contractDefinitionStore);
    }

    private ContractDefinition contractDefinition(Map<String, Object> privateProperties) {
        return ContractDefinition.Builder.newInstance()
                .id("definitionId")
                .accessPolicyId("policyId")
                .contractPolicyId("policyId")
                .participantContextId("participantContextId")
                .privateProperties(privateProperties)
                .build();
    }

    private ContractNegotiation negotiation(ContractNegotiation.Type type, int state) {
        var offer = ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create("definitionId", "assetId").toString())
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();

        return ContractNegotiation.Builder.newInstance()
                .participantContextId("participantContextId")
                .type(type)
                .state(state)
                .counterPartyId("any")
                .counterPartyAddress("any")
                .protocol("any")
                .contractOffer(offer)
                .build();
    }
}
