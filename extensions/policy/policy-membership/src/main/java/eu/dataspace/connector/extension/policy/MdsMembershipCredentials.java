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
 * Finds the MDS MembershipCredentials presented by a counterparty.
 * <p>
 * The credentials found in the participant agent's {@value #VC_CLAIM} claim have already been verified by the DCP
 * identity service (trusted issuer, subject bound to the presentation holder, validity period, revocation status).
 * <p>
 * JWT credentials (VC2_0_JOSE, as used by MDS) are not JSON-LD expanded by EDC, so {@code type} holds the terms as
 * written by the issuer. A credential is an MDS MembershipCredential when its type expands to
 * {@value #MDS_MEMBERSHIP_CREDENTIAL_IRI}, that is when either:
 * <ul>
 *     <li>it is typed with the full IRI (e.g. an expanded LDP credential), or</li>
 *     <li>it is typed {@code MembershipCredential} and its {@code @context} contains {@value #MDS_CREDENTIALS_CONTEXT}:
 *     that context is {@code @protected} and maps the term to the MDS IRI.</li>
 * </ul>
 * A {@code MembershipCredential} of another dataspace (without the MDS credentials context) is not accepted.
 */
public class MdsMembershipCredentials {

    static final String VC_CLAIM = "vc";
    static final String MDS_CREDENTIALS_NAMESPACE = "https://w3id.org/mobility-dataspace/2026/1/credentials/";
    static final String MDS_CREDENTIALS_CONTEXT = MDS_CREDENTIALS_NAMESPACE + "context.jsonld";
    static final String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";
    static final String MDS_MEMBERSHIP_CREDENTIAL_IRI = MDS_CREDENTIALS_NAMESPACE + MEMBERSHIP_CREDENTIAL_TYPE;

    /**
     * Returns the MDS MembershipCredentials presented by the participant agent.
     *
     * @param participantAgent the counterparty
     * @return a non-empty list of MDS MembershipCredentials, or a failure when the counterparty presented none
     */
    public Result<List<VerifiableCredential>> find(ParticipantAgent participantAgent) {
        if (!(participantAgent.getClaims().get(VC_CLAIM) instanceof Collection<?> credentials)) {
            return Result.failure("No verifiable credentials presented by the counterparty");
        }

        var membershipCredentials = credentials.stream()
                .filter(VerifiableCredential.class::isInstance)
                .map(VerifiableCredential.class::cast)
                .filter(this::isMdsMembershipCredential)
                .toList();

        if (membershipCredentials.isEmpty()) {
            return Result.failure("No MDS MembershipCredential presented by the counterparty");
        }

        return Result.success(membershipCredentials);
    }

    private boolean isMdsMembershipCredential(VerifiableCredential credential) {
        var types = credential.getType();
        return types.contains(MDS_MEMBERSHIP_CREDENTIAL_IRI) ||
                (types.contains(MEMBERSHIP_CREDENTIAL_TYPE) && credential.getContext().contains(MDS_CREDENTIALS_CONTEXT));
    }

}
