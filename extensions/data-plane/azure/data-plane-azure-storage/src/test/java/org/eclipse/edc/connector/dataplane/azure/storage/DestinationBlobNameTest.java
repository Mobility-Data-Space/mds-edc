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
 *       Marco Pirmo (BMW AG)
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DestinationBlobNameTest {

    @Test
    void shouldReturnBlobName_whenFolderNameIsEmptyAndPartSizeIsEqualToOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "";
        var expected = "blobName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldReturnPartName_whenFolderNameIsEmptyAndPartSizeIsGraterThanOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "";
        var expected = "partName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsEmpty() {

        var partName = "partName";
        var blobName = "";
        var folderName = "folderName";
        var expected = "folderName/partName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }


    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsEmptyAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var blobName = "";
        var folderName = "folderName/";
        var expected = "folderName/partName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldUsePartName_whenBothBlobNameAndFolderNameIsEmpty() {

        var partName = "partName";
        var blobName = "";
        var folderName = "";
        var expected = "partName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName";
        var expected = "folderName/blobName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOneAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName/";
        var expected = "folderName/blobName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName";
        var expected = "folderName/partName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOneAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName";
        var expected = "folderName/partName";
        var destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

}
