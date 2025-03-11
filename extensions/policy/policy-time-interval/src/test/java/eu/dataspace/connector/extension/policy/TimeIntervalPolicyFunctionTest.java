package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeIntervalPolicyFunctionTest {

    private final Supplier<OffsetDateTime> currentDateProvider = mock();
    private final TestPolicyContext context = new TestPolicyContext();
    private final TimeIntervalPolicyFunction<Rule, ParticipantAgentPolicyContext> function =
            new TimeIntervalPolicyFunction<>(currentDateProvider);

    @ParameterizedTest
    @ArgumentsSource(ValidConditions.class)
    void shouldSucceed_whenConditionIsMet(OffsetDateTime now, Operator operator, OffsetDateTime policyTime) {
        when(currentDateProvider.get()).thenReturn(now);

        var result = function.evaluate(operator, policyTime.toString(), null, context);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidConditions.class)
    void shouldFail_whenConditionIsUnmet(OffsetDateTime now, Operator operator, OffsetDateTime policyTime) {
        when(currentDateProvider.get()).thenReturn(now);

        var result = function.evaluate(operator, policyTime.toString(), null, context);

        assertThat(result).isFalse();
    }

    @Test
    void shouldFailWhenRightValueIsNotDate() {
        var result = function.evaluate(Operator.EQ, "any", null, context);

        assertThat(result).isFalse();
        assertThat(context.hasProblems()).isTrue();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Failed to parse right value of constraint to date."));
    }

    @Test
    void shouldFailWhenOperatorNotSupported() {
        var result = function.evaluate(Operator.IN, OffsetDateTime.now().toString(), null, context);

        assertThat(result).isFalse();
        assertThat(context.hasProblems()).isTrue();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Operator 'IN' not supported"));
    }

    private static class ValidConditions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            var now = OffsetDateTime.now();
            return Stream.of(
                    arguments(now, Operator.EQ, now),
                    arguments(now, Operator.NEQ, now.plus(1, MILLIS)),
                    arguments(now, Operator.GT, now.minus(1, MILLIS)),
                    arguments(now, Operator.GEQ, now),
                    arguments(now, Operator.GEQ, now.minus(1, MILLIS)),
                    arguments(now, Operator.LT, now.plus(1, MILLIS)),
                    arguments(now, Operator.LEQ, now),
                    arguments(now, Operator.LEQ, now.plus(1, MILLIS))
            );
        }
    }

    private static class InvalidConditions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            var now = OffsetDateTime.now();
            return Stream.of(
                    arguments(now, Operator.EQ, now.plus(1, MILLIS)),
                    arguments(now, Operator.NEQ, now),
                    arguments(now, Operator.GT, now),
                    arguments(now, Operator.GEQ, now.plus(1, MILLIS)),
                    arguments(now, Operator.LT, now),
                    arguments(now, Operator.LEQ, now.minus(1, MILLIS))
            );
        }
    }

    private class TestPolicyContext extends PolicyContextImpl implements ParticipantAgentPolicyContext {

        private final ParticipantAgent participantAgent = new ParticipantAgent(emptyMap(), emptyMap());

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
