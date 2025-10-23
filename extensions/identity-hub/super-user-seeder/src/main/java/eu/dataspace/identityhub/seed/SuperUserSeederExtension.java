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

package eu.dataspace.identityhub.seed;

import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.List;
import java.util.Map;

/**
 * Creates a super-user if it doesn't exist giving the configured settings
 */
public class SuperUserSeederExtension implements ServiceExtension {

    @Setting(
            key = "edc.identityhub.superuser.id",
            description = "Super-user's participant ID.",
            defaultValue = "super-user"
    )
    public String superUserId;

    @Setting(
            key = "edc.identityhub.superuser.api.key",
            description = "Initial API key for the Super-User"
    )
    public String superUserApiKey;

    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private Vault vault;
    @Inject
    private Monitor monitor;

    @Override
    public void start() {
        if (participantContextService.getParticipantContext(superUserId).succeeded()) {
            monitor.debug("super-user already exists with ID '%s', will not re-create".formatted(superUserId));
            return;
        }

        var manifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(superUserId)
                .did("did:web:%s".formatted(superUserId)) // doesn't matter, not intended for resolution
                .active(true)
                .key(KeyDescriptor.Builder.newInstance()
                        .keyGeneratorParams(Map.of("algorithm", "EdDSA", "curve", "Ed25519"))
                        .keyId("%s-key".formatted(superUserId))
                        .privateKeyAlias("%s-alias".formatted(superUserId))
                        .build())
                .roles(List.of("admin"))
                .build();

        participantContextService.createParticipantContext(manifest)
                .orElseThrow(failure -> new EdcException("Error creating super-user [%s]: %s".formatted(superUserId, failure.getFailureDetail())));

        participantContextService.getParticipantContext(superUserId)
                .onSuccess(participantContext -> vault.storeSecret(participantContext.getApiTokenAlias(), superUserApiKey))
                .orElseThrow(failure -> new EdcException("Failed setting api key for super-user [%s]: %s".formatted(superUserId, failure.getFailureDetail())));
    }
}
