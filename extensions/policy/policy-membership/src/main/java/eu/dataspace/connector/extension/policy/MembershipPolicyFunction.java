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

/**
 * Evaluates the MDS {@code Membership} constraint, as specified by the MDS ODRL profile 2026/1:
 * satisfied when, and only when, the counterparty presents an MDS MembershipCredential.
 * <p>
 * Only {@code eq "active"} is allowed. The credential's presence is evaluated; no claim is read.
 * <p>
 * The credentials found in the participant agent's {@value #VC_CLAIM} claim have already been verified by the DCP
 * identity service (trusted issuer, subject bound to the presentation holder, validity period, revocation status),
 * so this function only has to look for an MDS MembershipCredential.
 * <p>
 * JWT credentials (VC2_0_JOSE, as used by MDS) are not JSON-LD expanded by EDC, so {@code type} holds the terms as
 * written by the issuer. A credential is an MDS MembershipCredential when its type expands to
 * {@value #MDS_MEMBERSHIP_CREDENTIAL_IRI}, that is when either:
 * <ul>
 *     <li>it is typed with the full IRI (e.g. an expanded LDP credential), or</li>
 *     <li>it is typed {@code MembershipCredential} and its {@code @context} contains {@value #MDS_CREDENTIALS_CONTEXT}:
 *     that context is {@code @protected} and maps the term to the MDS IRI.</li>
 * </ul>
 * A {@code MembershipCredential} of another dataspace (without the MDS credentials context) is not accepted.
 */
public class MembershipPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    static final String VC_CLAIM = "vc";
    static final String MDS_CREDENTIALS_NAMESPACE = "https://w3id.org/mobility-dataspace/2026/1/credentials/";
    static final String MDS_CREDENTIALS_CONTEXT = MDS_CREDENTIALS_NAMESPACE + "context.jsonld";
    static final String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";
    static final String MDS_MEMBERSHIP_CREDENTIAL_IRI = MDS_CREDENTIALS_NAMESPACE + MEMBERSHIP_CREDENTIAL_TYPE;
    static final String ACTIVE = "active";

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
                .anyMatch(this::isMdsMembershipCredential);

        if (!hasMembershipCredential) {
            context.reportProblem(PROBLEM_PREFIX + "No MDS MembershipCredential presented by the counterparty");
        }

        return hasMembershipCredential;
    }

    private boolean isMdsMembershipCredential(VerifiableCredential credential) {
        var types = credential.getType();
        return types.contains(MDS_MEMBERSHIP_CREDENTIAL_IRI) ||
                (types.contains(MEMBERSHIP_CREDENTIAL_TYPE) && credential.getContext().contains(MDS_CREDENTIALS_CONTEXT));
    }

}
