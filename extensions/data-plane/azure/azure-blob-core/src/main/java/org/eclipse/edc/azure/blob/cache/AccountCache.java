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

import com.azure.storage.blob.BlobServiceClient;

public interface AccountCache {


    /**
     * Initially, confirms if account is stored in cache and, if so, returns it. If not, saves the account in cache and retrieves it.
     *
     * @param accountName The name of the storage account.
     * @param accountKey  The key of the storage account
     * @return The blob service client corresponding to the client to a storage account.
     */
    BlobServiceClient getBlobServiceClient(String accountName, String accountKey);

    /**
     * Initially, confirms if account is stored in cache and, if so, returns it. If not, resolved the account key and saves the account in cache and retrieves it.
     *
     * @param accountName The name of the storage account.
     * @return The blob service client corresponding to the client to a storage account.
     */
    BlobServiceClient getBlobServiceClient(String accountName);
}
