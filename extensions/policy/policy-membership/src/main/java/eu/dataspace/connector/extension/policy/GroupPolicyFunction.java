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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static eu.dataspace.connector.extension.policy.MdsCredentials.MDS_CREDENTIALS_NAMESPACE;
import static java.util.stream.Collectors.toSet;

/**
 * Evaluates the MDS {@code Group} constraint, as specified by the MDS ODRL profile 2026/1:
 * the MDS groups the counterparty belongs to, read from the {@code credentialSubject.groups} of its MDS
 * GroupMembershipCredential, a string of group identifiers separated by {@code ,}.
 * <p>
 * Allowed operators and right operands:
 * <ul>
 *     <li>{@code eq}: one group identifier, a non-empty string without {@code ,}</li>
 *     <li>{@code isAnyOf}, {@code isAllOf}: one or more group identifiers, a list of non-empty strings without {@code ,}
 *     (a single string is accepted as a one-element list)</li>
 * </ul>
 * The constraint is satisfied when one GroupMembershipCredential presented by the counterparty lists the group
 * ({@code eq}), at least one of the groups ({@code isAnyOf}) or all of them ({@code isAllOf}).
 *
 * @see MdsCredentials
 */
public class GroupPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    static final String GROUPS_CLAIM = "groups";
    static final String GROUPS_CLAIM_IRI = MDS_CREDENTIALS_NAMESPACE + GROUPS_CLAIM;

    private static final String PROBLEM_PREFIX = "Failing evaluation of Group constraint. ";

    private final MdsCredentials groupMembershipCredentials;

    public GroupPolicyFunction(MdsCredentials groupMembershipCredentials) {
        this.groupMembershipCredentials = groupMembershipCredentials;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        var expected = switch (operator) {
            case EQ -> singleGroup(rightValue, context);
            case IS_ANY_OF, IS_ALL_OF -> groups(rightValue, context);
            default -> {
                context.reportProblem(PROBLEM_PREFIX + "Unsupported operator: '%s', only 'eq', 'isAnyOf' and 'isAllOf' are allowed"
                        .formatted(operator.getOdrlRepresentation()));
                yield null;
            }
        };

        if (expected == null) {
            return false;
        }

        var credentials = groupMembershipCredentials.find(context.participantAgent());
        if (credentials.failed()) {
            context.reportProblem(PROBLEM_PREFIX + credentials.getFailureDetail());
            return false;
        }

        var groupSets = credentials.getContent().stream()
                .map(this::groupsOf)
                .filter(Objects::nonNull)
                .toList();

        if (groupSets.isEmpty()) {
            context.reportProblem(PROBLEM_PREFIX + "No groups found in the counterparty's MDS GroupMembershipCredential");
            return false;
        }

        var satisfied = groupSets.stream().anyMatch(groups -> switch (operator) {
            case IS_ALL_OF -> groups.containsAll(expected);
            default -> expected.stream().anyMatch(groups::contains);
        });

        if (!satisfied) {
            context.reportProblem(PROBLEM_PREFIX + "Counterparty groups %s do not satisfy '%s' %s"
                    .formatted(groupSets, operator.getOdrlRepresentation(), expected));
        }

        return satisfied;
    }

    /**
     * The groups listed by a credential, or null when it lists none.
     */
    private Set<String> groupsOf(VerifiableCredential credential) {
        var groups = credential.getCredentialSubject().stream()
                .map(CredentialSubject::getClaims)
                .filter(Objects::nonNull)
                .flatMap(claims -> Stream.of(claims.get(GROUPS_CLAIM), claims.get(GROUPS_CLAIM_IRI)))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .filter(group -> !group.isEmpty())
                .collect(toSet());
        return groups.isEmpty() ? null : groups;
    }

    private Set<String> singleGroup(Object rightValue, C context) {
        if (rightValue instanceof String group && isValidGroup(group)) {
            return Set.of(group);
        }
        context.reportProblem(PROBLEM_PREFIX + "Right operand must be one group identifier, a non-empty string without ',', but was '%s'".formatted(rightValue));
        return null;
    }

    private Set<String> groups(Object rightValue, C context) {
        var values = rightValue instanceof String group ? List.of(group) : rightValue;
        if (values instanceof Collection<?> collection && !collection.isEmpty()
                && collection.stream().allMatch(it -> it instanceof String group && isValidGroup(group))) {
            return collection.stream().map(String.class::cast).collect(toSet());
        }
        context.reportProblem(PROBLEM_PREFIX + "Right operand must be one or more group identifiers, non-empty strings without ',', but was '%s'".formatted(rightValue));
        return null;
    }

    private boolean isValidGroup(String group) {
        return !group.isBlank() && !group.contains(",");
    }

}
