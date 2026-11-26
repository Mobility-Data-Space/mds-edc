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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.List;

/**
 * Finds the MDS credentials of one type (e.g. MembershipCredential) presented by a counterparty.
 * <p>
 * The credentials found in the participant agent's {@value #VC_CLAIM} claim have already been verified by the DCP
 * identity service (trusted issuer, subject bound to the presentation holder, validity period, revocation status).
 * <p>
 * JWT credentials (VC2_0_JOSE, as used by MDS) are not JSON-LD expanded by EDC, so {@code type} holds the terms as
 * written by the issuer. A credential is an MDS credential of type {@code T} when its type expands to
 * {@value #MDS_CREDENTIALS_NAMESPACE}{@code T}, that is when either:
 * <ul>
 *     <li>it is typed with the full IRI (e.g. an expanded LDP credential), or</li>
 *     <li>it is typed {@code T} and its {@code @context} contains {@value #MDS_CREDENTIALS_CONTEXT}:
 *     that context is {@code @protected} and maps the term to the MDS IRI.</li>
 * </ul>
 * A credential of another dataspace with the same type name (without the MDS credentials context) is not accepted.
 */
public class MdsCredentials {

    static final String VC_CLAIM = "vc";
    static final String MDS_CREDENTIALS_NAMESPACE = "https://w3id.org/mobility-dataspace/2026/1/credentials/";
    static final String MDS_CREDENTIALS_CONTEXT = MDS_CREDENTIALS_NAMESPACE + "context.jsonld";
    static final String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";
    static final String GROUP_MEMBERSHIP_CREDENTIAL_TYPE = "GroupMembershipCredential";

    private final String type;
    private final String iri;

    /**
     * The MDS MembershipCredentials.
     */
    public static MdsCredentials membership() {
        return new MdsCredentials(MEMBERSHIP_CREDENTIAL_TYPE);
    }

    /**
     * The MDS GroupMembershipCredentials.
     */
    public static MdsCredentials groupMembership() {
        return new MdsCredentials(GROUP_MEMBERSHIP_CREDENTIAL_TYPE);
    }

    MdsCredentials(String type) {
        this.type = type;
        this.iri = MDS_CREDENTIALS_NAMESPACE + type;
    }

    /**
     * Returns the MDS credentials of this type presented by the participant agent.
     *
     * @param participantAgent the counterparty
     * @return a non-empty list of MDS credentials, or a failure when the counterparty presented none
     */
    public Result<List<VerifiableCredential>> find(ParticipantAgent participantAgent) {
        if (!(participantAgent.getClaims().get(VC_CLAIM) instanceof Collection<?> credentials)) {
            return Result.failure("No verifiable credentials presented by the counterparty");
        }

        var mdsCredentials = credentials.stream()
                .filter(VerifiableCredential.class::isInstance)
                .map(VerifiableCredential.class::cast)
                .filter(this::isMdsCredential)
                .toList();

        if (mdsCredentials.isEmpty()) {
            return Result.failure("No MDS %s presented by the counterparty".formatted(type));
        }

        return Result.success(mdsCredentials);
    }

    private boolean isMdsCredential(VerifiableCredential credential) {
        var types = credential.getType();
        return types.contains(iri) ||
                (types.contains(type) && credential.getContext().contains(MDS_CREDENTIALS_CONTEXT));
    }

}
