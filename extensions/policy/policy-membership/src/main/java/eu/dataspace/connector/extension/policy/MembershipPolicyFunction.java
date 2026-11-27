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

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;

/**
 * Evaluates the MDS {@code Membership} constraint, as specified by the MDS ODRL profile 2026/1:
 * satisfied when, and only when, the counterparty presents an MDS MembershipCredential.
 * <p>
 * Only {@code eq "active"} is allowed. The credential's presence is evaluated; no claim is read.
 *
 * @see MdsCredentials
 */
public class MembershipPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    static final String ACTIVE = "active";

    private static final String PROBLEM_PREFIX = "Failing evaluation of Membership constraint. ";

    private final MdsCredentials mdsCredentials;

    public MembershipPolicyFunction(MdsCredentials mdsCredentials) {
        this.mdsCredentials = mdsCredentials;
    }

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

        var membershipCredentials = mdsCredentials.find(context.participantAgent());
        if (membershipCredentials.failed()) {
            context.reportProblem(PROBLEM_PREFIX + membershipCredentials.getFailureDetail());
            return false;
        }

        return true;
    }

}
