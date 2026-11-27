package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static eu.dataspace.connector.extension.negotiation.manual.approval.logic.ManualApprovalValidator.MANUAL_APPROVAL_PROPERTY;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ManualApprovalValidatorTest {

    private final Monitor monitor = mock();
    private final ManualApprovalValidator validator = new ManualApprovalValidator(monitor);
    private final Policy policy = Policy.Builder.newInstance().build();

    @Test
    void shouldFail_whenContractDefinitionRequiresManualApproval() {
        var context = context(contractDefinition(Map.of(MANUAL_APPROVAL_PROPERTY, "true")));

        var result = validator.apply(policy, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("requires manual approval"));
        verify(monitor).warning(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "any"})
    void shouldSucceed_whenManualApprovalPropertyIsNotTrue(String value) {
        var context = context(contractDefinition(Map.of(MANUAL_APPROVAL_PROPERTY, value)));

        var result = validator.apply(policy, context);

        assertThat(result).isTrue();
        assertThat(context.hasProblems()).isFalse();
        verifyNoInteractions(monitor);
    }

    @Test
    void shouldSucceed_whenManualApprovalPropertyIsMissing() {
        var context = context(contractDefinition(Map.of()));

        assertThat(validator.apply(policy, context)).isTrue();
    }

    @Test
    void shouldSucceed_whenContractDefinitionIsNotAvailable() {
        var context = context(null);

        assertThat(validator.apply(policy, context)).isTrue();
    }

    private ApprovalContractNegotiationPolicyContext context(ContractDefinition contractDefinition) {
        return new ApprovalContractNegotiationPolicyContext("participantContextId",
                new ParticipantAgent("any", emptyMap(), emptyMap()), null, contractDefinition);
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
}
