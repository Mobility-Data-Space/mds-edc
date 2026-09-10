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

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.util.string.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.azure.blob.utils.BlobStoreUtils.createEndpoint;

public class AccountCacheImpl implements AccountCache {

    private final Vault vault;
    private final Map<String, BlobServiceClient> accountCache = new HashMap<>();
    private final String endpointTemplate;

    public AccountCacheImpl(Vault vault, String endpointTemplate) {
        this.vault = vault;
        this.endpointTemplate = endpointTemplate;
    }

    @Override
    public BlobServiceClient getBlobServiceClient(String accountName) {
        if (isAccountInCache(accountName)) {
            return getAccount(accountName);
        }

        var accountKey = vault.resolveSecret(accountName);

        return saveAccount(accountName, accountKey);
    }

    @Override
    public BlobServiceClient getBlobServiceClient(String accountName, String accountKey) {
        if (StringUtils.isNullOrBlank(accountKey)) {
            return getBlobServiceClient(accountName);
        }

        if (isAccountInCache(accountName)) {
            return getAccount(accountName);
        }

        return saveAccount(accountName, accountKey);
    }

    private BlobServiceClient getAccount(String accountName) {
        return accountCache.get(accountName);
    }

    private boolean isAccountInCache(String accountName) {
        Objects.requireNonNull(accountName, "accountName");
        return accountCache.containsKey(accountName);
    }

    private BlobServiceClient saveAccount(String accountName, String accountKey) {
        var endpoint = createEndpoint(endpointTemplate, accountName);

        var blobServiceClient = accountKey == null ?
                new BlobServiceClientBuilder().credential(new DefaultAzureCredentialBuilder().build())
                        .endpoint(endpoint)
                        .buildClient() :
                new BlobServiceClientBuilder().credential(new StorageSharedKeyCredential(accountName, accountKey))
                        .endpoint(endpoint)
                        .buildClient();

        accountCache.put(accountName, blobServiceClient);
        return blobServiceClient;
    }
}