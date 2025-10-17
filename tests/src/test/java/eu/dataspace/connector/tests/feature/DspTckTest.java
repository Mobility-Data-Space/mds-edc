/*
 * Copyright (c) 2025 Mobility Data Space
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *      Think-it GmbH - initial API and implementation
 */

package eu.dataspace.connector.tests.feature;

import eu.dataspace.connector.tests.MdsParticipant;
import eu.dataspace.connector.tests.MdsParticipantFactory;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.dsp.system.DspSystemLauncher;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspacetck.core.api.system.SystemsConstants.TCK_LAUNCHER;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class DspTckTest {

    private static final LazySupplier<URI> WEBHOOK = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/tck"));
    private static final String TEST_PACKAGE = "org.eclipse.dataspacetck.dsp.verification";

    @RegisterExtension
    protected static MdsParticipant runtime = MdsParticipantFactory.tck("CUT")
            .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                    entry("web.http.tck.port", String.valueOf(WEBHOOK.get().getPort())),
                    entry("web.http.tck.path", WEBHOOK.get().getPath()),
                    entry("edc.participant.id", "participantContextId"),
                    entry("web.http.port", "8080"),
                    entry("web.http.path", "/api"),
                    entry("web.http.control.port", String.valueOf(getFreePort())),
                    entry("web.http.control.path", "/api/control"),
                    entry("edc.management.context.enabled", "true"),
                    entry("edc.component.id", "DSP-compatibility-test")
            )));

    @Timeout(300)
    @Test
    void assertDspCompatibility() throws IOException {
        var monitor = new ConsoleMonitor(true, true);

        var result = TckRuntime.Builder.newInstance()
                .properties(loadProperties())
                .property(TCK_LAUNCHER, DspSystemLauncher.class.getName())
                .property("dataspacetck.debug", "true")
                .addPackage(TEST_PACKAGE)
                .monitor(monitor)
                .build()
                .execute();

        assertThat(result.getTestsStartedCount()).isGreaterThan(0);

        var failures = result.getFailures().stream().map(this::mapFailure);

        assertThat(failures).isEmpty();
    }

    private Map<String, String> loadProperties() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(resourceConfig("tck/tck.properties"))) {
            properties.load(reader);
        }

        properties.put("dataspacetck.dsp.connector.http.url", runtime.getProtocolUrl() + "/2025-1");
        properties.put("dataspacetck.dsp.connector.http.base.url", runtime.getProtocolUrl());
        properties.put("dataspacetck.dsp.connector.negotiation.initiate.url", WEBHOOK.get() + "/negotiations/requests");
        properties.put("dataspacetck.dsp.connector.transfer.initiate.url", WEBHOOK.get() + "/transfers/requests");
        properties.put("dataspacetck.dsp.connector.agent.id", "participantContextId");
        properties.put("dataspacetck.dsp.connector.http.headers.authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}");
        properties.put("dataspacetck.dsp.jsonld.context.edc.uri", "https://w3id.org/edc/dspace/v0.0.1");
        properties.put("dataspacetck.dsp.jsonld.context.edc.path", resourceConfig("tck/dspace-edc-context-v1.jsonld"));

        return properties.entrySet().stream()
                .collect(toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    private String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }

    private TestResult mapFailure(TestExecutionSummary.Failure failure) {
        var displayName = failure.getTestIdentifier().getDisplayName().split(":");
        return new TestResult(format("%s:%s", displayName[0], displayName[1]), failure);
    }

    private record TestResult(String testId, TestExecutionSummary.Failure failure) {

        @Override
        public @NotNull String toString() {
            return "- " + failure.getTestIdentifier().getDisplayName() + " (" + failure.getException() + ")";
        }
    }

}
