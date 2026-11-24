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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;

import java.util.Collection;
import java.util.Set;

/**
 * Evaluates the MDS {@code Membership} constraint, as specified by the MDS ODRL profile 2026/1:
 * satisfied when, and only when, the counterparty presents an MDS MembershipCredential.
 * <p>
 * Only {@code eq "active"} is allowed. The credential's presence is evaluated; no claim is read.
 * <p>
 * The credentials found in the participant agent's {@value #VC_CLAIM} claim have already been verified by the DCP
 * identity service (trusted issuer, subject bound to the presentation holder, validity period, revocation status),
 * so this function only has to look for one of the MembershipCredential type.
 */
public class MembershipPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    static final String VC_CLAIM = "vc";
    static final String MDS_CREDENTIALS_NAMESPACE = "https://w3id.org/mobility-dataspace/2026/1/credentials/";
    static final String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";
    static final String ACTIVE = "active";

    private static final Set<String> ACCEPTED_TYPES = Set.of(MEMBERSHIP_CREDENTIAL_TYPE, MDS_CREDENTIALS_NAMESPACE + MEMBERSHIP_CREDENTIAL_TYPE);
    private static final String PROBLEM_PREFIX = "Failing evaluation of Membership constraint. ";

    @Override
    public boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        if (operator != Operator.EQ) {
            context.reportProblem(PROBLEM_PREFIX + "Unsupported operator: '%s', only 'eq' is allowed".formatted(operator.getOdrlRepresentation()));
            return false;
        }

        if (!ACTIVE.equals(rightValue)) {
            context.reportProblem(PROBLEM_PREFIX + "Right operand must be the string '%s' but was '%s'".formatted(ACTIVE, rightValue));
            return false;
        }

        if (!(context.participantAgent().getClaims().get(VC_CLAIM) instanceof Collection<?> credentials)) {
            context.reportProblem(PROBLEM_PREFIX + "No verifiable credentials presented by the counterparty");
            return false;
        }

        var hasMembershipCredential = credentials.stream()
                .filter(VerifiableCredential.class::isInstance)
                .map(VerifiableCredential.class::cast)
                .anyMatch(credential -> credential.getType().stream().anyMatch(ACCEPTED_TYPES::contains));

        if (!hasMembershipCredential) {
            context.reportProblem(PROBLEM_PREFIX + "No MembershipCredential presented by the counterparty");
        }

        return hasMembershipCredential;
    }

}
