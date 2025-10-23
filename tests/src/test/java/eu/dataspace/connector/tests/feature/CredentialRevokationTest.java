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

package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.Issuer;
import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import eu.dataspace.connector.tests.Wallet;
import eu.dataspace.connector.tests.extensions.PostgresqlExtension;
import eu.dataspace.connector.tests.extensions.VaultExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CredentialRevokationTest {

    @RegisterExtension
    @Order(0)
    private static final VaultExtension VAULT_EXTENSION = new VaultExtension();

    @RegisterExtension
    @Order(0)
    private static final PostgresqlExtension POSTGRES_EXTENSION = new PostgresqlExtension("issuer", "wallet");

    @RegisterExtension
    @Order(1)
    private static final Issuer ISSUER = MdsParticipantFactory.issuer(POSTGRES_EXTENSION, VAULT_EXTENSION);

    @RegisterExtension
    @Order(2)
    private static final Wallet IDENTITY_HUB = MdsParticipantFactory.wallet(POSTGRES_EXTENSION, VAULT_EXTENSION, "consumer", "provider");

    @RegisterExtension
    @Order(3)
    private static final MdsParticipant PROVIDER = MdsParticipantFactory.inMemoryDcp("provider", IDENTITY_HUB, ISSUER.did());

    @RegisterExtension
    @Order(3)
    private static final MdsParticipant CONSUMER = MdsParticipantFactory.inMemoryDcp("consumer", IDENTITY_HUB, ISSUER.did());


    @Disabled // TODO: revocation cannot be requested because of: https://github.com/eclipse-edc/IdentityHub/issues/898
    @Test
    void shouldRevokeCredentials() {
        ISSUER.registerAttestationAndCredentialDefinition();
        ISSUER.registerHolder(PROVIDER.getId(), PROVIDER.getName());
        ISSUER.registerHolder(CONSUMER.getId(), CONSUMER.getName());
        IDENTITY_HUB.requestCredentialIssuance(PROVIDER.getId(), ISSUER.did().get());
        IDENTITY_HUB.requestCredentialIssuance(CONSUMER.getId(), ISSUER.did().get());

        CONSUMER.getCatalog(PROVIDER).statusCode(200);

        ISSUER.revokeCredentials(CONSUMER.getId()).statusCode(200);

        CONSUMER.getCatalog(PROVIDER).statusCode(502);
    }
}
