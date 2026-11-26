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

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static eu.dataspace.connector.extension.policy.ParticipantIdPolicyExtension.NAME;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;

/**
 * Registers the MDS {@code ParticipantId} constraint function on catalog, contract negotiation and transfer process scopes.
 * <p>
 * Example:
 * <pre>
 * { "leftOperand": "ParticipantId", "operator": "isAnyOf", "rightOperand": ["did:web:consumer.example"] }
 * </pre>
 * with the {@value MdsPolicy#MDS_POLICY_CONTEXT} context.
 */
@Extension(NAME)
public class ParticipantIdPolicyExtension implements ServiceExtension {

    static final String NAME = "MDS ParticipantId Policy";
    static final String PARTICIPANT_ID_CONSTRAINT_KEY = MdsPolicy.MDS_POLICY_NAMESPACE + "ParticipantId";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    private final MdsCredentials mdsCredentials = MdsCredentials.membership();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFunctionAndBindTo(CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE);
        registerFunctionAndBindTo(ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE);
        registerFunctionAndBindTo(TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE);
    }

    private <C extends ParticipantAgentPolicyContext> void registerFunctionAndBindTo(Class<C> contextClass, String scope) {
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, scope);
        ruleBindingRegistry.bind(PARTICIPANT_ID_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Permission.class, PARTICIPANT_ID_CONSTRAINT_KEY, new ParticipantIdPolicyFunction<>(mdsCredentials));
        policyEngine.registerFunction(contextClass, Duty.class, PARTICIPANT_ID_CONSTRAINT_KEY, new ParticipantIdPolicyFunction<>(mdsCredentials));
        policyEngine.registerFunction(contextClass, Prohibition.class, PARTICIPANT_ID_CONSTRAINT_KEY, new ParticipantIdPolicyFunction<>(mdsCredentials));
    }

}
