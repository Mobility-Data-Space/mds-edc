/*
 * Copyright (c) 2025 Mobility Data Space
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

package eu.dataspace.connector.dcp;

import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class DcpExtension implements ServiceExtension {

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // TODO: should the first part something related to MDS instead of generic org.eclipse.edc.vc.type?
        var defaultScopes = Set.of("org.eclipse.edc.vc.type:MembershipCredential:read");
        var defaultScopesProtocols = Map.of(
                "dataspace-protocol-http", defaultScopes, // use DSP_SCOPE_V_08 constant in dsp-spi
                "dataspace-protocol-http:2025-1", defaultScopes // use DSP_SCOPE_V_2025_1 constant in dsp-spi
        );

        policyEngine.registerPostValidator(RequestCatalogPolicyContext.class, new DefaultScopeExtractor<>(defaultScopesProtocols));
        policyEngine.registerPostValidator(RequestContractNegotiationPolicyContext.class, new DefaultScopeExtractor<>(defaultScopesProtocols));
        policyEngine.registerPostValidator(RequestTransferProcessPolicyContext.class, new DefaultScopeExtractor<>(defaultScopesProtocols));
    }

    public record DefaultScopeExtractor<C extends RequestPolicyContext>(
            Map<String, Set<String>> defaultScopes) implements PolicyValidatorRule<C> {

        @Override
        public Boolean apply(Policy policy, RequestPolicyContext policyContext) {
            var scopes = policyContext.requestScopeBuilder();
            if (scopes == null) {
                throw new EdcException(format("%s not set in policy context", RequestScope.Builder.class.getName()));
            }

            var protocol = java.util.Optional.of(policyContext)
                    .map(RequestPolicyContext::requestContext)
                    .map(RequestContext::getMessage)
                    .map(RemoteMessage::getProtocol)
                    .orElse(null);

            defaultScopes.get(protocol).forEach(scopes::scope);

            return true;
        }
    }

}
