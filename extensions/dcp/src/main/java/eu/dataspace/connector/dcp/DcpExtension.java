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

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.List;

/**
 * Registers the MembershipCredential scope as a DEFAULT DCP scope, so it is requested on every outgoing DSP
 * message, regardless of the policy constraints. Policy-based {@code ScopeExtractor}s are only invoked per
 * constraint, so they cannot provide scopes for unconstrained requests (e.g. catalog/dataset requests).
 */
public class DcpExtension implements ServiceExtension {

    static final String MEMBERSHIP_SCOPE = "org.eclipse.dspace.dcp.vc.type:MembershipCredential:read";
    static final List<String> PROTOCOLS = List.of(
            "dataspace-protocol-http", // DSP_SCOPE_V_08 in dsp-spi
            "dataspace-protocol-http:2025-1" // DSP_SCOPE_V_2025_1 in dsp-spi
    );

    @Inject
    private DcpScopeRegistry scopeRegistry;

    @Override
    public void prepare() {
        PROTOCOLS.forEach(protocol -> {
            var scope = DcpScope.Builder.newInstance()
                    .id("mds-membership-" + protocol)
                    .type(DcpScope.Type.DEFAULT)
                    .value(MEMBERSHIP_SCOPE)
                    .profile(protocol)
                    .build();

            scopeRegistry.register(scope)
                    .orElseThrow(failure -> new EdcException("Cannot register default DCP scope %s: %s"
                            .formatted(scope.getId(), failure.getFailureDetail())));
        });
    }

}
