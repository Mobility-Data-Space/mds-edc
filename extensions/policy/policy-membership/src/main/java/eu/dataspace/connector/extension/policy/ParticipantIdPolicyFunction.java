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
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Evaluates the MDS {@code ParticipantId} constraint, as specified by the MDS ODRL profile 2026/1:
 * the counterparty's {@code did:web} DID, read from the {@code credentialSubject.id} of its MDS MembershipCredential.
 * <p>
 * Allowed operators and right operands:
 * <ul>
 *     <li>{@code eq}, {@code neq}: one DID, a non-empty string without {@code ,}</li>
 *     <li>{@code isAnyOf}, {@code isNoneOf}: one or more DIDs, a list of non-empty strings (a single string is accepted as a one-element list)</li>
 * </ul>
 * {@code neq} and {@code isNoneOf} exclude participants from the MDS members: they are never satisfied for a
 * counterparty that does not present an MDS MembershipCredential.
 *
 * @see MdsCredentials
 */
public class ParticipantIdPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    private static final String PROBLEM_PREFIX = "Failing evaluation of ParticipantId constraint. ";

    private final MdsCredentials mdsCredentials;

    public ParticipantIdPolicyFunction(MdsCredentials mdsCredentials) {
        this.mdsCredentials = mdsCredentials;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        var expected = switch (operator) {
            case EQ, NEQ -> singleDid(rightValue, context);
            case IS_ANY_OF, IS_NONE_OF -> dids(rightValue, context);
            default -> {
                context.reportProblem(PROBLEM_PREFIX + "Unsupported operator: '%s', only 'eq', 'neq', 'isAnyOf' and 'isNoneOf' are allowed"
                        .formatted(operator.getOdrlRepresentation()));
                yield null;
            }
        };

        if (expected == null) {
            return false;
        }

        var membershipCredentials = mdsCredentials.find(context.participantAgent());
        if (membershipCredentials.failed()) {
            context.reportProblem(PROBLEM_PREFIX + membershipCredentials.getFailureDetail());
            return false;
        }

        var participantIds = membershipCredentials.getContent().stream()
                .flatMap(credential -> credential.getCredentialSubject().stream())
                .map(CredentialSubject::getId)
                .filter(Objects::nonNull)
                .collect(toSet());

        if (participantIds.isEmpty()) {
            context.reportProblem(PROBLEM_PREFIX + "No participant id found in the counterparty's MDS MembershipCredential");
            return false;
        }

        var satisfied = switch (operator) {
            case EQ, IS_ANY_OF -> participantIds.stream().anyMatch(expected::contains);
            default -> participantIds.stream().noneMatch(expected::contains);
        };

        if (!satisfied) {
            context.reportProblem(PROBLEM_PREFIX + "Counterparty participant id %s does not satisfy '%s' %s"
                    .formatted(participantIds, operator.getOdrlRepresentation(), expected));
        }

        return satisfied;
    }

    private Set<String> singleDid(Object rightValue, C context) {
        if (rightValue instanceof String did && isValidDid(did)) {
            return Set.of(did);
        }
        context.reportProblem(PROBLEM_PREFIX + "Right operand must be one DID, a non-empty string without ',', but was '%s'".formatted(rightValue));
        return null;
    }

    private Set<String> dids(Object rightValue, C context) {
        var values = rightValue instanceof String did ? List.of(did) : rightValue;
        if (values instanceof Collection<?> collection && !collection.isEmpty()
                && collection.stream().allMatch(it -> it instanceof String did && isValidDid(did))) {
            return collection.stream().map(String.class::cast).collect(toSet());
        }
        context.reportProblem(PROBLEM_PREFIX + "Right operand must be one or more DIDs, non-empty strings without ',', but was '%s'".formatted(rightValue));
        return null;
    }

    private boolean isValidDid(String did) {
        return !did.isBlank() && !did.contains(",");
    }

}
