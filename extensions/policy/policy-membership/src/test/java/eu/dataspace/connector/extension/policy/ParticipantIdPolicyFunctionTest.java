/*
 * Copyright (c) 2026 Mobility Data Space
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      Think-it GmbH - initial API and implementation
 */

package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ParticipantIdPolicyFunctionTest {

    private static final String CONSUMER = "did:web:consumer.example";
    private static final String OTHER = "did:web:other.example";

    private final MdsMembershipCredentials mdsMembershipCredentials = mock();
    private final ParticipantIdPolicyFunction<Rule, ParticipantAgentPolicyContext> function = new ParticipantIdPolicyFunction<>(mdsMembershipCredentials);

    @Nested
    class Eq {

        @Test
        void shouldReturnTrue_whenSubjectIdMatches() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.EQ, CONSUMER, null, context);

            assertThat(result).isTrue();
            assertThat(context.hasProblems()).isFalse();
        }

        @Test
        void shouldReturnFalse_whenSubjectIdDoesNotMatch() {
            var context = contextWithCredentials(mdsMembershipCredential(OTHER));

            var result = function.evaluate(Operator.EQ, CONSUMER, null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("does not satisfy"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "did:web:a.example,did:web:b.example"})
        void shouldReturnFalse_whenRightOperandIsNotOneDid(String rightOperand) {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.EQ, rightOperand, null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }

        @Test
        void shouldReturnFalse_whenRightOperandIsList() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.EQ, List.of(CONSUMER), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }
    }

    @Nested
    class Neq {

        @Test
        void shouldReturnTrue_whenSubjectIdDiffers() {
            var context = contextWithCredentials(mdsMembershipCredential(OTHER));

            var result = function.evaluate(Operator.NEQ, CONSUMER, null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenSubjectIdMatches() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.NEQ, CONSUMER, null, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenNoMembershipCredentialIsPresented() {
            var context = contextWithoutMembershipCredential();

            var result = function.evaluate(Operator.NEQ, CONSUMER, null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("No MDS MembershipCredential"));
        }
    }

    @Nested
    class IsAnyOf {

        @Test
        void shouldReturnTrue_whenSubjectIdIsInList() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(OTHER, CONSUMER), null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnTrue_whenRightOperandIsSingleString() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_ANY_OF, CONSUMER, null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenSubjectIdIsNotInList() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(OTHER), null, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenRightOperandIsEmptyList() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }

        @Test
        void shouldReturnFalse_whenRightOperandContainsInvalidDid() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(CONSUMER, ""), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }
    }

    @Nested
    class IsNoneOf {

        @Test
        void shouldReturnTrue_whenSubjectIdIsNotInList() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_NONE_OF, List.of(OTHER), null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenSubjectIdIsInList() {
            var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

            var result = function.evaluate(Operator.IS_NONE_OF, List.of(OTHER, CONSUMER), null, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenNoMembershipCredentialIsPresented() {
            var context = contextWithoutMembershipCredential();

            var result = function.evaluate(Operator.IS_NONE_OF, List.of(OTHER), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("No MDS MembershipCredential"));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = {"EQ", "NEQ", "IS_ANY_OF", "IS_NONE_OF"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalse_whenOperatorIsNotSupported(Operator operator) {
        var context = contextWithCredentials(mdsMembershipCredential(CONSUMER));

        var result = function.evaluate(operator, CONSUMER, null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Unsupported operator"));
        verifyNoInteractions(mdsMembershipCredentials);
    }

    @Test
    void shouldReturnTrue_whenOneOfMultipleMembershipCredentialsMatches() {
        var context = contextWithCredentials(mdsMembershipCredential(OTHER), mdsMembershipCredential(CONSUMER));

        var result = function.evaluate(Operator.EQ, CONSUMER, null, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenMembershipCredentialHasNoSubjectId() {
        var context = contextWithCredentials(mdsMembershipCredential(null));

        var result = function.evaluate(Operator.IS_NONE_OF, List.of(OTHER), null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No participant id"));
    }

    private TestPolicyContext contextWithCredentials(VerifiableCredential... credentials) {
        when(mdsMembershipCredentials.find(any())).thenReturn(Result.success(List.of(credentials)));
        return new TestPolicyContext();
    }

    private TestPolicyContext contextWithoutMembershipCredential() {
        when(mdsMembershipCredentials.find(any())).thenReturn(Result.failure("No MDS MembershipCredential presented by the counterparty"));
        return new TestPolicyContext();
    }

    private VerifiableCredential mdsMembershipCredential(String subjectId) {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", "MembershipCredential"))
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id(subjectId).claim("holderIdentifier", "consumer").build())
                .build();
    }
}
