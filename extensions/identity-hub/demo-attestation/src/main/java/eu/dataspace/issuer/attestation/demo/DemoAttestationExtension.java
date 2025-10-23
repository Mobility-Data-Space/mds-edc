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

package eu.dataspace.issuer.attestation.demo;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.ValidationResult;

import java.util.Map;

public class DemoAttestationExtension implements ServiceExtension {

    @Inject
    private AttestationSourceFactoryRegistry attestationSourceFactoryRegistry;

    @Inject
    private AttestationDefinitionValidatorRegistry attestationDefinitionValidatorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // TODO: use DatabaseAttestation (first we need a db though :') )
        attestationSourceFactoryRegistry.registerFactory("demo", new DemoAttestationSourceFactory());

        attestationDefinitionValidatorRegistry.registerValidator("demo", ignored -> ValidationResult.success());
    }

    private static class DemoAttestationSourceFactory implements AttestationSourceFactory {
        @Override
        public AttestationSource createSource(AttestationDefinition definition) {
            return context -> Result.success(Map.of(
                    "onboarding", Map.of("signedDocuments", true),
                    "participant", Map.of("name", "Alice")) // TODO: should this be changed?
            );
        }
    }
}
