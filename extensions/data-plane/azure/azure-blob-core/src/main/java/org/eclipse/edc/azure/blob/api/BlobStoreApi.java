/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.azure.blob.api;

import com.azure.core.credential.AzureSasCredential;
import com.azure.storage.blob.models.BlobItem;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.time.OffsetDateTime;
import java.util.List;

@ExtensionPoint
public interface BlobStoreApi {

    void createContainer(String accountName, String containerName);

    void deleteContainer(String accountName, String containerName);

    boolean exists(String accountName, String containerName);

    String createContainerSasToken(String accountName, String containerName, String accessSpec, OffsetDateTime expiry);

    List<BlobItem> listContainer(String accountName, String containerName);

    /**
     * List all blobs from given folder in the container on a storage account.
     *
     * @param accountName The name of the storage account
     * @param containerName The name of the container within the storage account
     * @param directory The name of the folder within the container of the storage account
     * @param accountKey The key of the storage account
     * @return Lazy loaded list of blobs from folder specified by the input parameters
     */
    List<BlobItem> listContainerFolder(String accountName, String containerName, String directory, String accountKey);

    void putBlob(String accountName, String containerName, String blobName, byte[] data);

    String createAccountSas(String accountName, String containerName, String racwxdl, OffsetDateTime expiry);

    byte[] getBlob(String account, String container, String blobName);

    /**
     * Get a blob adapter containing convenience methods for working on blob objects on a storage account.
     * This method accepts storage account key credential, and it is used in a context, where unlimited access to
     * a storage account is required.
     *
     * @param accountName The name of the storage account
     * @param containerName The name of the container within the storage account
     * @param blobName The name of the blob within the container of the storage account
     * @param sharedKey The storage account key credential
     * @return The blob adapter corresponding to the blob specified by the input parameters
     */
    BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, String sharedKey);

    /**
     * Get a blob adapter containing convenience methods for working on blob objects on a storage account.
     * This method accepts a SAS (Shared Access Signature) token as a credential, and it's typically used for accessing
     * a storage account with limited sets of privileges. Pls. refer to the Azure SAS documentation for further details.
     *
     * @param accountName The name of the storage account
     * @param containerName The name of the container within the storage account
     * @param blobName The name of the blob within the container of the storage account
     * @param credential A valid SAS token
     * @return The blob adapter corresponding to the blob specified by the input parameters
     */
    BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, AzureSasCredential credential);

    /**
     * Get a blob adapter containing convenience methods for working on blob objects on a storage account.
     * This method doesn't require any credentials; it uses {@link com.azure.identity.DefaultAzureCredentialBuilder},
     * which contains an authentication flow consisting of several authentication mechanisms to be tried in a specific
     * order. Pls. refer to the official documentation for further details, i.e. the list of mechanisms tried.
     *
     * @param accountName The name of the storage account
     * @param containerName The name of the container within the storage account
     * @param blobName The name of the blob within the container of the storage account
     * @return The blob adapter corresponding to the blob specified by the input parameters
     */
    BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName);
}
