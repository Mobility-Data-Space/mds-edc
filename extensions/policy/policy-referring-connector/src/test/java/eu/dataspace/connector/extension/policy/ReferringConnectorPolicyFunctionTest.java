package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static eu.dataspace.connector.extension.policy.ReferringConnectorPolicyFunction.REFERRING_CONNECTOR_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;

class ReferringConnectorPolicyFunctionTest {

    private final ReferringConnectorPolicyFunction<Rule, ParticipantAgentPolicyContext> function =
            new ReferringConnectorPolicyFunction<>();


    @Nested
    class Eq {
        @Test
        void shouldReturnTrue_whenReferringIsEqualToRightValue() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.EQ, "referring", null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenReferringIsContainedInRightValueWithOtherValues() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.EQ, "other,referring,another", null, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenReferringIsNotEqualToRightValue() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.EQ, "another", null, context);

            assertThat(result).isFalse();
            assertThat(context.hasProblems()).isFalse();
        }

        @Test
        void shouldFailWhenRightValueNotString() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.EQ, 3, null, context);

            assertThat(result).isFalse();
            assertThat(context.hasProblems()).isTrue();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand must be a String"));
        }
    }

    @Nested
    class In {

        @Test
        void shouldReturnTrue_whenReferringIsContainedInRightValue() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.IN, "another,referring,other", null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenReferringIsNotContainedInRightValue() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.IN, "another,other", null, context);

            assertThat(result).isFalse();
            assertThat(context.hasProblems()).isFalse();
        }

        @Test
        void shouldReturnFalse_whenRightValueNotString() {
            var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
            var context = new TestPolicyContext(participantAgent);

            var result = function.evaluate(Operator.IN, 4, null, context);

            assertThat(result).isFalse();
            assertThat(context.hasProblems()).isTrue();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand must be a String"));
        }

    }

    @Test
    void shouldFailWhenClaimIsMissing() {
        var participantAgent = new ParticipantAgent(Collections.emptyMap(), Collections.emptyMap());
        var context = new TestPolicyContext(participantAgent);

        var result = function.evaluate(Operator.EQ, "any", null, context);

        assertThat(result).isFalse();
        assertThat(context.hasProblems()).isTrue();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("is null"));
    }

    @Test
    void shouldFailWhenClaimIsNotString() {
        var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, 3), Collections.emptyMap());
        var context = new TestPolicyContext(participantAgent);

        var result = function.evaluate(Operator.EQ, "any", null, context);

        assertThat(result).isFalse();
        assertThat(context.hasProblems()).isTrue();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("not a String"));
    }

    @Test
    void shouldFailWhenClaimIsAnEmptyString() {
        var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, ""), Collections.emptyMap());
        var context = new TestPolicyContext(participantAgent);

        var result = function.evaluate(Operator.EQ, "any", null, context);

        assertThat(result).isFalse();
        assertThat(context.hasProblems()).isTrue();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("empty string"));
    }

    @Test
    void shouldFailWhenOperatorNotSupported() {
        var participantAgent = new ParticipantAgent(Map.of(REFERRING_CONNECTOR_CLAIM, "referring"), Collections.emptyMap());
        var context = new TestPolicyContext(participantAgent);

        var result = function.evaluate(Operator.GEQ, "any", null, context);

        assertThat(result).isFalse();
        assertThat(context.hasProblems()).isTrue();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Unsupported operator"));
    }

    private class TestPolicyContext extends PolicyContextImpl implements ParticipantAgentPolicyContext {

        private final ParticipantAgent participantAgent;

        private TestPolicyContext(ParticipantAgent participantAgent) {
            this.participantAgent = participantAgent;
        }

        @Override
        public ParticipantAgent participantAgent() {
            return participantAgent;
        }

        @Override
        public String scope() {
            return "test";
        }
    }
}
