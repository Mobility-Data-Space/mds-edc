/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.azure.blob.adapter;

import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlockBlobOutputStreamOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.implementation.Constants;
import org.eclipse.edc.azure.blob.BlobStorageConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Implementation of {@link BlobAdapter} using a {@link BlockBlobClient}.
 */
public class DefaultBlobAdapter implements BlobAdapter {
    private final BlobStorageConfiguration blobStorageConfiguration;
    private final BlockBlobClient client;

    public DefaultBlobAdapter(BlockBlobClient client, BlobStorageConfiguration blobStorageConfiguration) {
        this.client = client;
        this.blobStorageConfiguration = blobStorageConfiguration;
    }

    @Override
    public OutputStream getOutputStream() {
        var parallelTransferOptions = new ParallelTransferOptions()
                .setBlockSizeLong(blobStorageConfiguration.blockSize() * Constants.MB)
                .setMaxConcurrency(blobStorageConfiguration.maxConcurrency())
                .setMaxSingleUploadSizeLong(blobStorageConfiguration.maxSingleUploadSize() * Constants.MB);

        return client.getBlobOutputStream(new BlockBlobOutputStreamOptions().setParallelTransferOptions(parallelTransferOptions));
    }

    @Override
    public InputStream openInputStream() {
        return client.openInputStream();
    }

    @Override
    public String getBlobName() {
        return client.getBlobName();
    }

    @Override
    public long getBlobSize() {
        return client.getProperties().getBlobSize();
    }

    @Override
    public void setMetadata(Map<String, String> metadata) {
        client.setMetadata(metadata);
    }
}
