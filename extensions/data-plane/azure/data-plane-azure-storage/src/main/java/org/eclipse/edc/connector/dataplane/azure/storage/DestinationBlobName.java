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

import org.eclipse.edc.util.string.StringUtils;

/**
 * Utility class responsible for determining the name under which a file will be saved.
 */
public class DestinationBlobName {

    private final String folderName;
    private final String blobName;

    public DestinationBlobName(String blobName, String folderName) {

        this.blobName = blobName;
        this.folderName = folderName;
    }

    /**
     * Resolves the name under which a resource should be saved based on its part name and size.
     *
     * @param partName  The name of the resource part.
     * @param partsSize The size of the resource part.
     * @return A String representing the resolved name for the resource.
     */
    public String resolve(String partName, int partsSize) {
        var name = "";
        if (!StringUtils.isNullOrBlank(folderName)) {
            if (folderName.endsWith("/")) {
                name += folderName;
            } else {
                name += folderName + "/";
            }
        }
        if (partsSize == 1 && !StringUtils.isNullOrBlank(blobName)) {
            name += blobName;
        } else {
            name += partName;
        }

        return name;
    }

}

