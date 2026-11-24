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
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static eu.dataspace.connector.extension.policy.MembershipPolicyFunction.VC_CLAIM;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class MembershipPolicyFunctionTest {

    private final MembershipPolicyFunction<Rule, ParticipantAgentPolicyContext> function = new MembershipPolicyFunction<>();

    @ParameterizedTest
    @ValueSource(strings = {"MembershipCredential", "https://w3id.org/mobility-dataspace/2026/1/credentials/MembershipCredential"})
    void shouldReturnTrue_whenMembershipCredentialIsPresented(String type) {
        var context = contextWithCredentials(credential("VerifiableCredential", type));

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isTrue();
        assertThat(context.hasProblems()).isFalse();
    }

    @Test
    void shouldReturnTrue_whenMembershipCredentialIsPresentedAmongOthers() {
        var context = contextWithCredentials(
                credential("VerifiableCredential", "GroupMembershipCredential"),
                credential("VerifiableCredential", "MembershipCredential"));

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenNoMembershipCredentialIsPresented() {
        var context = contextWithCredentials(credential("VerifiableCredential", "GroupMembershipCredential"));

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No MembershipCredential"));
    }

    @Test
    void shouldReturnFalse_whenTypeIsInAnotherNamespace() {
        var context = contextWithCredentials(credential("VerifiableCredential", "https://example.com/credentials/MembershipCredential"));

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenNoCredentialsArePresented() {
        var context = new TestPolicyContext(new ParticipantAgent("any", emptyMap(), emptyMap()));

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("No verifiable credentials"));
    }

    @Test
    void shouldReturnFalse_whenVcClaimDoesNotContainCredentials() {
        var context = new TestPolicyContext(new ParticipantAgent("any", Map.of(VC_CLAIM, List.of("MembershipCredential")), emptyMap()));

        var result = function.evaluate(Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = "EQ", mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalse_whenOperatorIsNotEq(Operator operator) {
        var context = contextWithCredentials(credential("VerifiableCredential", "MembershipCredential"));

        var result = function.evaluate(operator, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Unsupported operator"));
    }

    @Test
    void shouldReturnFalse_whenRightOperandIsNotActive() {
        var context = contextWithCredentials(credential("VerifiableCredential", "MembershipCredential"));

        var result = function.evaluate(Operator.EQ, "inactive", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
    }

    @Test
    void shouldReturnFalse_whenRightOperandIsNotString() {
        var context = contextWithCredentials(credential("VerifiableCredential", "MembershipCredential"));

        var result = function.evaluate(Operator.EQ, List.of("active"), null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).anyMatch(it -> it.contains("Right operand"));
    }

    private TestPolicyContext contextWithCredentials(VerifiableCredential... credentials) {
        var claims = Map.<String, Object>of(VC_CLAIM, List.of(credentials));
        return new TestPolicyContext(new ParticipantAgent("did:web:consumer", claims, emptyMap()));
    }

    private VerifiableCredential credential(String... types) {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of(types))
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim("holderIdentifier", "consumer").build())
                .build();
    }

    private static class TestPolicyContext extends PolicyContextImpl implements ParticipantAgentPolicyContext {

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
