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

import static eu.dataspace.connector.extension.policy.GroupPolicyFunction.GROUPS_CLAIM;
import static eu.dataspace.connector.extension.policy.GroupPolicyFunction.GROUPS_CLAIM_IRI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GroupPolicyFunctionTest {

    private static final String PARKING = "parking-operators";
    private static final String TRANSIT = "transit-authorities";
    private static final String OTHER = "other-group";

    private final MdsCredentials groupMembershipCredentials = mock();
    private final GroupPolicyFunction<Rule, ParticipantAgentPolicyContext> function = new GroupPolicyFunction<>(groupMembershipCredentials);

    @Nested
    class Eq {

        @Test
        void shouldReturnTrue_whenGroupIsListed() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING + "," + TRANSIT));

            var result = function.evaluate(Operator.EQ, TRANSIT, null, context);

            assertThat(result).isTrue();
            assertThat(context.hasProblems()).isFalse();
        }

        @Test
        void shouldReturnFalse_whenGroupIsNotListed() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING));

            var result = function.evaluate(Operator.EQ, TRANSIT, null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("do not satisfy"));
        }

        @Test
        void shouldReturnFalse_whenGroupIsOnlyAPrefixOfAListedOne() {
            var context = contextWithCredentials(groupMembershipCredential("parking-operators-eu"));

            var result = function.evaluate(Operator.EQ, PARKING, null, context);

            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", PARKING + "," + TRANSIT})
        void shouldReturnFalse_whenRightOperandIsNotOneGroup(String rightOperand) {
            var context = contextWithCredentials(groupMembershipCredential(PARKING + "," + TRANSIT));

            var result = function.evaluate(Operator.EQ, rightOperand, null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }

        @Test
        void shouldReturnFalse_whenRightOperandIsList() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING));

            var result = function.evaluate(Operator.EQ, List.of(PARKING), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }
    }

    @Nested
    class IsAnyOf {

        @Test
        void shouldReturnTrue_whenOneGroupIsListed() {
            var context = contextWithCredentials(groupMembershipCredential(TRANSIT));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(PARKING, TRANSIT), null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnTrue_whenRightOperandIsSingleString() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING));

            var result = function.evaluate(Operator.IS_ANY_OF, PARKING, null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenNoGroupIsListed() {
            var context = contextWithCredentials(groupMembershipCredential(OTHER));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(PARKING, TRANSIT), null, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalse_whenRightOperandIsEmptyList() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }

        @Test
        void shouldReturnFalse_whenRightOperandContainsInvalidGroup() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING));

            var result = function.evaluate(Operator.IS_ANY_OF, List.of(PARKING, PARKING + "," + TRANSIT), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        }
    }

    @Nested
    class IsAllOf {

        @Test
        void shouldReturnTrue_whenAllGroupsAreListed() {
            var context = contextWithCredentials(groupMembershipCredential(OTHER + "," + PARKING + "," + TRANSIT));

            var result = function.evaluate(Operator.IS_ALL_OF, List.of(PARKING, TRANSIT), null, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenSomeGroupIsNotListed() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING));

            var result = function.evaluate(Operator.IS_ALL_OF, List.of(PARKING, TRANSIT), null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).anyMatch(it -> it.contains("do not satisfy"));
        }

        @Test
        void shouldReturnFalse_whenGroupsAreSpreadOverMultipleCredentials() {
            var context = contextWithCredentials(groupMembershipCredential(PARKING), groupMembershipCredential(TRANSIT));

            var result = function.evaluate(Operator.IS_ALL_OF, List.of(PARKING, TRANSIT), null, context);

            assertThat(result).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = {"EQ", "IS_ANY_OF", "IS_ALL_OF"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalse_whenOperatorIsNotSupported(Operator operator) {
        var context = contextWithCredentials(groupMembershipCredential(PARKING));

        var result = function.evaluate(operator, PARKING, null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Unsupported operator"));
        verifyNoInteractions(groupMembershipCredentials);
    }

    @Test
    void shouldReturnFalse_whenNoGroupMembershipCredentialIsPresented() {
        when(groupMembershipCredentials.find(any())).thenReturn(Result.failure("No MDS GroupMembershipCredential presented by the counterparty"));
        var context = new TestPolicyContext();

        var result = function.evaluate(Operator.EQ, PARKING, null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No MDS GroupMembershipCredential"));
    }

    @Test
    void shouldReturnTrue_whenOneOfMultipleCredentialsMatches() {
        var context = contextWithCredentials(groupMembershipCredential(OTHER), groupMembershipCredential(PARKING));

        var result = function.evaluate(Operator.EQ, PARKING, null, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrue_whenGroupsClaimIsExpanded() {
        var credential = credentialWithSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim(GROUPS_CLAIM_IRI, PARKING).build());
        var context = contextWithCredentials(credential);

        var result = function.evaluate(Operator.EQ, PARKING, null, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenCredentialHasNoGroupsClaim() {
        var credential = credentialWithSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim("holderIdentifier", "consumer").build());
        var context = contextWithCredentials(credential);

        var result = function.evaluate(Operator.EQ, PARKING, null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No groups"));
    }

    @Test
    void shouldReturnFalse_whenGroupsClaimIsNotAString() {
        var credential = credentialWithSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim(GROUPS_CLAIM, List.of(PARKING)).build());
        var context = contextWithCredentials(credential);

        var result = function.evaluate(Operator.EQ, PARKING, null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No groups"));
    }

    private TestPolicyContext contextWithCredentials(VerifiableCredential... credentials) {
        when(groupMembershipCredentials.find(any())).thenReturn(Result.success(List.of(credentials)));
        return new TestPolicyContext();
    }

    private VerifiableCredential groupMembershipCredential(String groups) {
        return credentialWithSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim(GROUPS_CLAIM, groups).build());
    }

    private VerifiableCredential credentialWithSubject(CredentialSubject subject) {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", "GroupMembershipCredential"))
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(subject)
                .build();
    }
}
