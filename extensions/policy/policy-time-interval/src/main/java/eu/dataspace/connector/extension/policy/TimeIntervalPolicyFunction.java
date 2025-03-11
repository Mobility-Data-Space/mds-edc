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

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

/**
 * Time interval constraint validation function. Checks the time specified in the policy against the current date time.
 */
public class TimeIntervalPolicyFunction<R extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<R, C> {

    private final Supplier<OffsetDateTime> currentDateSupplier;

    public TimeIntervalPolicyFunction(Supplier<OffsetDateTime> currentDateSupplier) {
        this.currentDateSupplier = currentDateSupplier;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, R rule, C context) {
        try {
            var policyDate = OffsetDateTime.parse((String) rightValue);
            var nowDate = currentDateSupplier.get();
            return switch (operator) {
                case LT -> nowDate.isBefore(policyDate);
                case LEQ -> nowDate.isBefore(policyDate) || nowDate.equals(policyDate);
                case GT -> nowDate.isAfter(policyDate);
                case GEQ -> nowDate.isAfter(policyDate) || nowDate.equals(policyDate);
                case EQ -> nowDate.equals(policyDate);
                case NEQ -> !nowDate.equals(policyDate);
                default -> {
                    context.reportProblem("Operator '%s' not supported".formatted(operator));
                    yield false;
                }
            };
        } catch (DateTimeParseException e) {
            context.reportProblem("Failed to parse right value of constraint to date.");
            return false;
        }
    }

}
