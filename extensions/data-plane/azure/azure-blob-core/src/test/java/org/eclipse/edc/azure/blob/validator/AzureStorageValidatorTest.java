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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AzureStorageValidatorTest {


    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefghijabcdefghijbcde", "1er", "451", "ge45"})
    void validateAccountName_success(String input) {
        AzureStorageValidator.validateAccountName(input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "$log", "r", "re", "a a", " b", "ag_c", "re-r", "bdjfkCJdfd", "efer:a",
            "abcdefghijabcdefghijbcdef"})
    void validateAccountName_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateAccountName(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "$root", "$logs", "$web",
            "re-r", "z0r-a-q",
            "abc", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234567890", "1er", "451", "ge45"})
    void validateContainerName_success(String input) {
        AzureStorageValidator.validateContainerName(input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"$log", "  ", "r", "re", "a a", "-ree", "era-", "z0rr--", " b", "ag_c",
            "bdjfkCJdfd", "efer:a", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz012345678901"})
    void validateContainerName_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateContainerName(input));
    }

    @ParameterizedTest
    @ArgumentsSource(ValidBlobNameProvider.class)
    void validateBlobName_success(String input) {
        AzureStorageValidator.validateBlobName(input);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidBlobNameProvider.class)
    @NullSource
    void validateBlobName_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateBlobName(input));
    }

    @ParameterizedTest
    @ArgumentsSource(ValidBlobPrefixProvider.class)
    void validateBlobPrefix_success(String input) {
        AzureStorageValidator.validateBlobPrefix(input);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidBlobPrefixProvider.class)
    @NullSource
    void validateBlobPrefix_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateBlobPrefix(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {  "abcdefghijklmnop", "-", "a/%!_- $K1~"})
    void validateMetadata_success(String input) {
        AzureStorageValidator.validateMetadata(input);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidMetadataProvider.class)
    @NullAndEmptySource
    void validateMetadata_fail(String input) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AzureStorageValidator.validateMetadata(input));
    }

    private static class InvalidBlobNameProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(""),
                    Arguments.of("abcdefghijklmnop".repeat(64) + "a"),
                    Arguments.of("a/b".repeat(254)));
        }
    }

    private static class ValidBlobNameProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of("geq"),
                    Arguments.of("Qja143"),
                    Arguments.of("ABE"),
                    Arguments.of("a name"),
                    Arguments.of("end space "),
                    Arguments.of("je`~3j4k%$':\\"),
                    Arguments.of("abcdefghijklmnop".repeat(64)),
                    Arguments.of("a/b".repeat(253)));
        }
    }

    private static class InvalidBlobPrefixProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(""),
                    Arguments.of("dfhjdhfjhsd"),
                    Arguments.of("abcdefghijklmnop".repeat(64) + "/"),
                    Arguments.of("a/b".repeat(253) + "/")
                    );
        }
    }

    private static class ValidBlobPrefixProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of("geq/"),
                    Arguments.of("Qja143/"),
                    Arguments.of("ABE/"),
                    Arguments.of("a name/"),
                    Arguments.of("end space /"),
                    Arguments.of("je`~3j4k%$':\\/"),
                    Arguments.of("abcdefghijklmnop".repeat(63) + "/"),
                    Arguments.of("a/b".repeat(252) + "/"));
        }
    }

    private static class InvalidMetadataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of("abcdefghijklmnop".repeat(256) + " a"),
                    Arguments.of("a/%!_- $KÄ".repeat(64)));
        }
    }
}
