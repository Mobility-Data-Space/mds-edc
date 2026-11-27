package eu.dataspace.connector.extension.negotiation.manual.approval.logic;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ManualApprovalPolicyFunctionTest {

    private final ManualApprovalPolicyFunction function = new ManualApprovalPolicyFunction();
    private final Permission permission = Permission.Builder.newInstance().build();

    @Test
    void evaluate_shouldNotBeSatisfied_soThatNegotiationIsHeldForManualApproval() {
        var context = context();

        var result = function.evaluate(Operator.EQ, "true", permission, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("requires manual approval"));
    }

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = "EQ", mode = EnumSource.Mode.EXCLUDE)
    void evaluate_shouldFail_whenOperatorIsNotEq(Operator operator) {
        var context = context();

        var result = function.evaluate(operator, "true", permission, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Unsupported operator"));
    }

    @Test
    void evaluate_shouldFail_whenRightOperandIsNotTrue() {
        var context = context();

        var result = function.evaluate(Operator.EQ, "false", permission, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand must be the string 'true'"));
    }

    @Test
    void validate_shouldSucceed_whenConstraintFollowsProfile() {
        assertThat(function.validate(Operator.EQ, "true", permission)).isSucceeded();
    }

    @Test
    void validate_shouldFail_whenConstraintDoesNotFollowProfile() {
        assertThat(function.validate(Operator.NEQ, "true", permission)).isFailed();
        assertThat(function.validate(Operator.EQ, "false", permission)).isFailed();
        assertThat(function.validate(Operator.EQ, true, permission)).isFailed();
        assertThat(function.validate(Operator.EQ, List.of("true"), permission)).isFailed();
    }

    private ApprovalContractNegotiationPolicyContext context() {
        return new ApprovalContractNegotiationPolicyContext("participantContextId", new ParticipantAgent("any", emptyMap(), emptyMap()));
    }
}
