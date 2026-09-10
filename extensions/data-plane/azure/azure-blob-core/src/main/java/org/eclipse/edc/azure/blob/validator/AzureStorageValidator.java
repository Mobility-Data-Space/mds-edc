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

package org.eclipse.edc.azure.blob.validator;


import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.string.StringUtils;

import java.util.regex.Pattern;


/**
 * Validates storage account resource names and keys.
 * <p>
 * See <a href="https://docs.microsoft.com/rest/api/storageservices/naming-and-referencing-containers--blobs--and-metadata">
 * Azure documentation</a>.
 */
public class AzureStorageValidator {
    private static final int ACCOUNT_MIN_LENGTH = 3;
    private static final int ACCOUNT_MAX_LENGTH = 24;
    private static final int CONTAINER_MIN_LENGTH = 3;
    private static final int CONTAINER_MAX_LENGTH = 63;
    private static final int BLOB_MIN_LENGTH = 1;
    private static final int BLOB_MAX_LENGTH = 1024;
    private static final int METADATA_MIN_LENGTH = 1;
    private static final int METADATA_MAX_LENGTH = 4096;
    private static final int PATH_SEGMENTS_MAX = 254;
    private static final Pattern ACCOUNT_REGEX = Pattern.compile("^[a-z0-9]+$");
    private static final Pattern CONTAINER_REGEX = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern METADATA_REGEX = Pattern.compile("^[ -~]*$"); // US-ASCII

    private static final String ACCOUNT = "account";
    private static final String BLOB = "blob";
    private static final String CONTAINER = "container";
    private static final String KEY_NAME = "keyName";
    private static final String METADATA = "metadata";
    private static final String PREFIX = "prefix";

    private static final String INVALID_PREFIX = "Invalid %s prefix, prefix must end with a '/' character";
    private static final String INVALID_RESOURCE_NAME = "Invalid %s name";
    private static final String INVALID_RESOURCE_NAME_LENGTH = "Invalid %s name length, the name must be between %s and %s characters long";
    private static final String RESOURCE_NAME_EMPTY = "Invalid %s name, the name may not be null, empty or blank";
    private static final String TOO_MANY_PATH_SEGMENTS = "The number of URL path segments (strings between '/' characters) as part of the blob name cannot exceed %s.";

    /**
     * Checks if an account name is valid.
     *
     * @param accountName A String representing the account name to validate.
     * @throws IllegalArgumentException if the string does not represent a valid account name.
     */
    public static void validateAccountName(String accountName) {
        checkLength(accountName, ACCOUNT, ACCOUNT_MIN_LENGTH, ACCOUNT_MAX_LENGTH);

        if (!ACCOUNT_REGEX.matcher(accountName).matches()) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME, ACCOUNT));
        }
    }

    /**
     * Checks if a container name is valid.
     *
     * @param containerName A String representing the container name to validate.
     * @throws IllegalArgumentException if the string does not represent a valid container name.
     */
    public static void validateContainerName(String containerName) {
        if (!("$root".equals(containerName) || "$logs".equals(containerName) || "$web".equals(containerName))) {
            checkLength(containerName, CONTAINER, CONTAINER_MIN_LENGTH, CONTAINER_MAX_LENGTH);

            if (!CONTAINER_REGEX.matcher(containerName).matches()) {
                throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME, CONTAINER));
            }
        }
    }

    /**
     * Checks if a blob name is valid.
     *
     * @param blobName A String representing the blob name to validate.
     * @throws IllegalArgumentException if the string does not represent a valid blob name.
     */
    public static void validateBlobName(String blobName) {
        checkLength(blobName, BLOB, BLOB_MIN_LENGTH, BLOB_MAX_LENGTH);
        checkSegments(blobName);
    }

    /**
     * Checks if a blob prefix is valid.
     * The restriction is based on Azure Blob Storage folder 'virtualization' which is base on the forward slash (/)
     * used in the blob path as delimiter. Prefix has to ends with '/'.
     *
     * @param blobPrefix A String representing the blob prefix to validate.
     * @throws IllegalArgumentException if the string does not represent a valid prefix name.
     */
    public static void validateBlobPrefix(String blobPrefix) {
        checkLength(blobPrefix, PREFIX, BLOB_MIN_LENGTH, BLOB_MAX_LENGTH);
        checkSegments(blobPrefix);

        if (!blobPrefix.endsWith("/")) {
            throw new IllegalArgumentException(String.format(INVALID_PREFIX, blobPrefix));
        }

    }

    /**
     * Checks if a metadata value is valid.
     * The restriction is based on allowed characters for HTTP header values. As there is no length restriction per
     * header field, a reasonable restriction of 4096 character is assumed to leave some space, considering an
     * overall length restriction for HTTP headers of approximately 8KB.
     *
     * @param metadata A String representing the metadata value to validate.
     * @throws IllegalArgumentException if the string does not represent a valid metadata value.
     */
    public static void validateMetadata(String metadata) {
        checkLength(metadata, METADATA, METADATA_MIN_LENGTH, METADATA_MAX_LENGTH);

        if (!METADATA_REGEX.matcher(metadata).matches()) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME, METADATA));
        }
    }

    /**
     * Checks if key name is valid.
     *
     * @param keyName A string representing blob key secret.
     * @throws IllegalArgumentException if the string is null or blank.
     */
    public static void validateKeyNameOrSecret(DataAddress keyName) {
        if (StringUtils.isNullOrBlank(keyName.getKeyName()) && StringUtils.isNullOrBlank(keyName.getStringProperty(DataAddress.EDC_DATA_ADDRESS_SECRET))) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME, KEY_NAME + " or " + DataAddress.EDC_DATA_ADDRESS_SECRET));
        }
    }

    private static void checkLength(String name, String resourceType, int minLength, int maxLength) {
        if (StringUtils.isNullOrBlank(name)) {
            throw new IllegalArgumentException(String.format(RESOURCE_NAME_EMPTY, resourceType));
        }

        if (name.length() < minLength || name.length() > maxLength) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_NAME_LENGTH, resourceType, minLength, maxLength));
        }
    }

    private static void checkSegments(String name) {
        var slashCount = name.chars().filter(ch -> ch == '/').count();

        if (slashCount >= PATH_SEGMENTS_MAX) {
            throw new IllegalArgumentException(String.format(TOO_MANY_PATH_SEGMENTS, PATH_SEGMENTS_MAX));
        }
    }
}
