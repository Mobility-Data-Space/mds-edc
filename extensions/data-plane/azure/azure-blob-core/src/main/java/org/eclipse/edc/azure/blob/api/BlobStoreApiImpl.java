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
import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import org.eclipse.edc.azure.blob.BlobStorageConfiguration;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.adapter.DefaultBlobAdapter;
import org.eclipse.edc.azure.blob.cache.AccountCache;
import org.eclipse.edc.azure.blob.cache.AccountCacheImpl;
import org.eclipse.edc.spi.security.Vault;

import java.time.OffsetDateTime;
import java.util.List;

import static org.eclipse.edc.azure.blob.utils.BlobStoreUtils.createEndpoint;

public class BlobStoreApiImpl implements BlobStoreApi {

    private final BlobStorageConfiguration blobStorageConfiguration;
    private final AccountCache accountCache;

    public BlobStoreApiImpl(Vault vault, BlobStorageConfiguration blobStorageConfiguration) {
        this.blobStorageConfiguration = blobStorageConfiguration;
        this.accountCache = new AccountCacheImpl(vault, this.blobStorageConfiguration.blobstoreEndpointTemplate());
    }

    @Override
    public void createContainer(String accountName, String containerName) {
        accountCache.getBlobServiceClient(accountName).createBlobContainer(containerName);
    }

    @Override
    public void deleteContainer(String accountName, String containerName) {
        accountCache.getBlobServiceClient(accountName).deleteBlobContainer(containerName);
    }

    @Override
    public boolean exists(String accountName, String containerName) {
        return accountCache.getBlobServiceClient(accountName).getBlobContainerClient(containerName).exists();
    }

    @Override
    public String createContainerSasToken(String accountName, String containerName, String permissionSpec, OffsetDateTime expiry) {
        var permissions = BlobContainerSasPermission.parse(permissionSpec);
        var values = new BlobServiceSasSignatureValues(expiry, permissions);
        return accountCache.getBlobServiceClient(accountName).getBlobContainerClient(containerName).generateSas(values);
    }

    @Override
    public List<BlobItem> listContainer(String accountName, String containerName) {
        return accountCache.getBlobServiceClient(accountName).getBlobContainerClient(containerName).listBlobs().stream().toList();
    }

    @Override
    public List<BlobItem> listContainerFolder(String accountName, String containerName, String directory, String accountKey) {
        var options = new ListBlobsOptions().setPrefix(directory);
        return accountCache.getBlobServiceClient(accountName, accountKey).getBlobContainerClient(containerName).listBlobs(options, null).stream().toList();
    }

    @Override
    public void putBlob(String accountName, String containerName, String blobName, byte[] data) {
        var blobServiceClient = accountCache.getBlobServiceClient(accountName);
        blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName).upload(BinaryData.fromBytes(data), true);
    }

    @Override
    public String createAccountSas(String accountName, String containerName, String permissionSpec, OffsetDateTime expiry) {
        var permissions = AccountSasPermission.parse(permissionSpec);

        var services = AccountSasService.parse("b");
        var resourceTypes = AccountSasResourceType.parse("co");
        var values = new AccountSasSignatureValues(expiry, permissions, services, resourceTypes);
        return accountCache.getBlobServiceClient(accountName).generateAccountSas(values);
    }

    @Override
    public byte[] getBlob(String account, String container, String blobName) {
        var client = accountCache.getBlobServiceClient(account);
        return client.getBlobContainerClient(container).getBlobClient(blobName).downloadContent().toBytes();
    }

    @Override
    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, String sharedKey) {
        var builder = new BlobServiceClientBuilder().credential(new StorageSharedKeyCredential(accountName, sharedKey));
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    @Override
    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, AzureSasCredential credential) {
        var builder = new BlobServiceClientBuilder().credential(credential);
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    @Override
    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName) {
        var builder = new BlobServiceClientBuilder().credential(new DefaultAzureCredentialBuilder().build());
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    private BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, BlobServiceClientBuilder builder) {
        var blobServiceClient = builder
                .endpoint(createEndpoint(this.blobStorageConfiguration.blobstoreEndpointTemplate(), accountName))
                .buildClient();

        var blockBlobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .getBlockBlobClient();

        return new DefaultBlobAdapter(blockBlobClient, blobStorageConfiguration);
    }
}
