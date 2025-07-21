/*
 * Copyright (c) 2024 Mobility Data Space
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       sovity GmbH - adaptation and modifications
 *       Think-it GmbH - additional implementation
 *
 */

package eu.dataspace.connector.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;

/**
 * ReferringConnector constraint validation function
 */
public class ReferringConnectorPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    static final String REFERRING_CONNECTOR_CLAIM = "referringConnector";

    private static final String PROBLEM_PREFIX = "Failing evaluation because of invalid referring connector constraint. ";

    /**
     * Evaluation function.
     *
     * @param operator operator of the constraint
     * @param rightValue right value fo the constraint, that contains a referring connector
     * @param context context of the policy with claims
     * @return true if claims are from the constrained referring connector
     */
    @Override
    public boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        var claims = context.participantAgent().getClaims();

        var referringConnectorClaim = claims.get(REFERRING_CONNECTOR_CLAIM);
        if (referringConnectorClaim == null) {
            context.reportProblem(PROBLEM_PREFIX + REFERRING_CONNECTOR_CLAIM + " claim is null");
            return false;
        }

        if (!(referringConnectorClaim instanceof String referringConnectorClaimString)) {
            context.reportProblem(PROBLEM_PREFIX + REFERRING_CONNECTOR_CLAIM + " claim is not a String as expected");
            return false;
        }

        if (referringConnectorClaimString.isBlank()) {
            context.reportProblem(PROBLEM_PREFIX + REFERRING_CONNECTOR_CLAIM + " claim is an empty string");
            return false;
        }

        if (!(rightValue instanceof String referringConnectorString)) {
            context.reportProblem(PROBLEM_PREFIX + "Right operand must be a String");
            return false;
        }

        return switch (operator) {
            case EQ -> referringConnectorString.equals(referringConnectorClaimString);
            case IN -> referringConnectorString.contains(referringConnectorClaimString);
            default -> {
                context.reportProblem((PROBLEM_PREFIX + "Unsupported operator: '%s'").formatted(operator.getOdrlRepresentation()));
                yield false;
            }
        };

    }

}
