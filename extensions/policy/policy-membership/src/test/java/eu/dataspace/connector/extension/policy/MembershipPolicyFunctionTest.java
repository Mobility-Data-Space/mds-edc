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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MembershipPolicyFunctionTest {

    private final MdsMembershipCredentials mdsMembershipCredentials = mock();
    private final MembershipPolicyFunction<Rule, ParticipantAgentPolicyContext> function = new MembershipPolicyFunction<>(mdsMembershipCredentials);

    @Test
    void shouldReturnTrue_whenMembershipCredentialIsPresented() {
        when(mdsMembershipCredentials.find(any())).thenReturn(Result.success(List.of(membershipCredential())));
        var context = new TestPolicyContext();

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isTrue();
        assertThat(context.hasProblems()).isFalse();
    }

    @Test
    void shouldReturnFalse_whenNoMembershipCredentialIsPresented() {
        when(mdsMembershipCredentials.find(any())).thenReturn(Result.failure("No MDS MembershipCredential presented by the counterparty"));
        var context = new TestPolicyContext();

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No MDS MembershipCredential"));
    }

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = "EQ", mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalse_whenOperatorIsNotEq(Operator operator) {
        var context = new TestPolicyContext();

        var result = function.evaluate(operator, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Unsupported operator"));
        verifyNoInteractions(mdsMembershipCredentials);
    }

    @Test
    void shouldReturnFalse_whenRightOperandIsNotActive() {
        var context = new TestPolicyContext();

        var result = function.evaluate(Operator.EQ, "inactive", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
        verifyNoInteractions(mdsMembershipCredentials);
    }

    @Test
    void shouldReturnFalse_whenRightOperandIsNotString() {
        var context = new TestPolicyContext();

        var result = function.evaluate(Operator.EQ, List.of("active"), null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
    }

    private VerifiableCredential membershipCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of("VerifiableCredential", "MembershipCredential"))
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim("holderIdentifier", "consumer").build())
                .build();
    }
}
