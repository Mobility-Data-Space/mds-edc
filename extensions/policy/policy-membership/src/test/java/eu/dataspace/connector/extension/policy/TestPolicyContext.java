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

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;

import static java.util.Collections.emptyMap;

class TestPolicyContext extends PolicyContextImpl implements ParticipantAgentPolicyContext {

    private final ParticipantAgent participantAgent = new ParticipantAgent("did:web:consumer", emptyMap(), emptyMap());

    @Override
    public ParticipantAgent participantAgent() {
        return participantAgent;
    }

    @Override
    public String scope() {
        return "test";
    }
}
