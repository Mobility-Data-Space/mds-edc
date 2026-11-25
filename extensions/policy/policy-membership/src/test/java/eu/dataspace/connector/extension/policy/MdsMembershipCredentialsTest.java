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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static eu.dataspace.connector.extension.policy.MdsMembershipCredentials.VC_CLAIM;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class MdsMembershipCredentialsTest {

    private static final String VC2_CONTEXT = "https://www.w3.org/ns/credentials/v2";
    private static final String MDS_CREDENTIALS_CONTEXT = "https://w3id.org/mobility-dataspace/2026/1/credentials/context.jsonld";

    private final MdsMembershipCredentials mdsMembershipCredentials = new MdsMembershipCredentials();

    @Test
    void shouldReturnCredential_whenTypedMembershipCredentialWithMdsContext() {
        var credential = credential(List.of(VC2_CONTEXT, MDS_CREDENTIALS_CONTEXT), "VerifiableCredential", "MembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(credential));

        assertThat(result).isSucceeded().isEqualTo(List.of(credential));
    }

    @Test
    void shouldReturnCredential_whenTypedWithMdsIri() {
        var credential = credential(List.of(VC2_CONTEXT), "VerifiableCredential", "https://w3id.org/mobility-dataspace/2026/1/credentials/MembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(credential));

        assertThat(result).isSucceeded().isEqualTo(List.of(credential));
    }

    @Test
    void shouldReturnOnlyMembershipCredentials_whenPresentedAmongOthers() {
        var group = credential(List.of(VC2_CONTEXT, MDS_CREDENTIALS_CONTEXT), "VerifiableCredential", "GroupMembershipCredential");
        var membership = credential(List.of(VC2_CONTEXT, MDS_CREDENTIALS_CONTEXT), "VerifiableCredential", "MembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(group, membership));

        assertThat(result).isSucceeded().isEqualTo(List.of(membership));
    }

    @Test
    void shouldFail_whenNoMembershipCredentialIsPresented() {
        var credential = credential(List.of(VC2_CONTEXT, MDS_CREDENTIALS_CONTEXT), "VerifiableCredential", "GroupMembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(credential));

        assertThat(result).isFailed().detail().contains("No MDS MembershipCredential");
    }

    @Test
    void shouldFail_whenMembershipCredentialHasNoMdsContext() {
        var credential = credential(List.of(VC2_CONTEXT), "VerifiableCredential", "MembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(credential));

        assertThat(result).isFailed().detail().contains("No MDS MembershipCredential");
    }

    @Test
    void shouldFail_whenMembershipCredentialHasAnotherDataspaceContext() {
        var credential = credential(List.of(VC2_CONTEXT, "https://example.com/other-dataspace/credentials/context.jsonld"),
                "VerifiableCredential", "MembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(credential));

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenTypeIsInAnotherNamespace() {
        var credential = credential(List.of(VC2_CONTEXT, MDS_CREDENTIALS_CONTEXT), "VerifiableCredential", "https://example.com/credentials/MembershipCredential");

        var result = mdsMembershipCredentials.find(agentWith(credential));

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenNoCredentialsArePresented() {
        var result = mdsMembershipCredentials.find(new ParticipantAgent("any", emptyMap(), emptyMap()));

        assertThat(result).isFailed().detail().contains("No verifiable credentials");
    }

    @Test
    void shouldFail_whenVcClaimDoesNotContainCredentials() {
        var agent = new ParticipantAgent("any", Map.of(VC_CLAIM, List.of("MembershipCredential")), emptyMap());

        var result = mdsMembershipCredentials.find(agent);

        assertThat(result).isFailed().detail().contains("No MDS MembershipCredential");
    }

    private ParticipantAgent agentWith(VerifiableCredential... credentials) {
        return new ParticipantAgent("did:web:consumer", Map.of(VC_CLAIM, List.of(credentials)), emptyMap());
    }

    private VerifiableCredential credential(List<Object> contexts, String... types) {
        return VerifiableCredential.Builder.newInstance()
                .contexts(contexts)
                .types(List.of(types))
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("did:web:consumer").claim("holderIdentifier", "consumer").build())
                .build();
    }
}
