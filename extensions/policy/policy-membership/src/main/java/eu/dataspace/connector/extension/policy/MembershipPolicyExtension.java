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

import static eu.dataspace.connector.extension.policy.MembershipPolicyExtension.NAME;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;

/**
 * Registers the MDS {@code Membership} constraint function on catalog, contract negotiation and transfer process scopes.
 * <p>
 * Example:
 * <pre>
 * { "leftOperand": "Membership", "operator": "eq", "rightOperand": "active" }
 * </pre>
 * with the {@value MdsPolicy#MDS_POLICY_CONTEXT} context.
 */
@Extension(NAME)
public class MembershipPolicyExtension implements ServiceExtension {

    static final String NAME = "MDS Membership Policy";
    static final String MEMBERSHIP_CONSTRAINT_KEY = MdsPolicy.MDS_POLICY_NAMESPACE + "Membership";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    private final MdsMembershipCredentials mdsMembershipCredentials = new MdsMembershipCredentials();

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
        ruleBindingRegistry.bind(MEMBERSHIP_CONSTRAINT_KEY, scope);
        policyEngine.registerFunction(contextClass, Permission.class, MEMBERSHIP_CONSTRAINT_KEY, new MembershipPolicyFunction<>(mdsMembershipCredentials));
        policyEngine.registerFunction(contextClass, Duty.class, MEMBERSHIP_CONSTRAINT_KEY, new MembershipPolicyFunction<>(mdsMembershipCredentials));
        policyEngine.registerFunction(contextClass, Prohibition.class, MEMBERSHIP_CONSTRAINT_KEY, new MembershipPolicyFunction<>(mdsMembershipCredentials));
    }

}
