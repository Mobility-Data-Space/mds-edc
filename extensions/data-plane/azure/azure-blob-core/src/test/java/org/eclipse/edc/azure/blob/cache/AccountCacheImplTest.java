/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.azure.blob.cache;

import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.utils.BlobStoreUtils.createEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AccountCacheImplTest {

    private static final String ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private static final String ACCOUNT_NAME = "test_account";
    private static final String OTHER_ACCOUNT_NAME = "other_test_account";
    private static final String ACCOUNT_KEY = "test_key";

    private final AccountCache accountCache = new AccountCacheImpl(mock(Vault.class), ENDPOINT_TEMPLATE);

    @Test
    void getAccount_withKey_succeeds() {
        var result = accountCache.getBlobServiceClient(ACCOUNT_NAME, ACCOUNT_KEY);
        assertThat(result.getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertEquals(result.getAccountUrl(), createEndpoint(ENDPOINT_TEMPLATE, ACCOUNT_NAME));
    }

    @Test
    void getAccount_succeeds() {
        var result = accountCache.getBlobServiceClient(ACCOUNT_NAME);
        assertThat(result.getAccountName()).isEqualTo(ACCOUNT_NAME);
        assertEquals(result.getAccountUrl(), createEndpoint(ENDPOINT_TEMPLATE, ACCOUNT_NAME));
    }

    @Test
    void getAccount_sameAccount_succeeds() {
        var firstAccount = accountCache.getBlobServiceClient(ACCOUNT_NAME);
        var secondAccount = accountCache.getBlobServiceClient(ACCOUNT_NAME);
        assertThat(firstAccount).isEqualTo(secondAccount);
    }

    @Test
    void getAccount_differentAccounts_succeeds() {
        var firstAccount = accountCache.getBlobServiceClient(ACCOUNT_NAME);
        var secondAccount = accountCache.getBlobServiceClient(OTHER_ACCOUNT_NAME);
        assertThat(firstAccount).isNotEqualTo(secondAccount);
    }

    @Test
    void getAccount_sameAccountWithKey_succeeds() {
        var firstAccount = accountCache.getBlobServiceClient(ACCOUNT_NAME, ACCOUNT_KEY);
        var secondAccount = accountCache.getBlobServiceClient(ACCOUNT_NAME, ACCOUNT_KEY);
        assertThat(firstAccount).isEqualTo(secondAccount);
    }

    @Test
    void getAccount_WithNullKey_succeeds() {
        var result = accountCache.getBlobServiceClient(ACCOUNT_NAME, null);
        assertThat(result.getAccountName()).isEqualTo(ACCOUNT_NAME);
    }
}