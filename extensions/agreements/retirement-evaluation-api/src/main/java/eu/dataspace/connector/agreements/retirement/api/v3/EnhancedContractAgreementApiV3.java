/*
 * Copyright (c) 2026 Mobility Data Space
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

package eu.dataspace.connector.agreements.retirement.api.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiSchema;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

import static eu.dataspace.connector.agreements.retirement.api.v3.EnhancedContractAgreementApiV3.EnhancedContractAgreementSchema.ENHANCED_CONTRACT_AGREEMENT_EXAMPLE;

@OpenAPIDefinition(info = @Info(version = "v3"))
@Tag(name = "Enhanced Contract Agreement V3")
public interface EnhancedContractAgreementApiV3 {

    @Operation(description = "Gets all enhanced contract agreements according to a particular query",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The contract agreements matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ManagementApiSchema.ContractAgreementSchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonArray getAllEnhancedAgreements(JsonObject querySpecJson);

    @Schema(name = "EnhancedContractAgreement", example = ENHANCED_CONTRACT_AGREEMENT_EXAMPLE)
    record EnhancedContractAgreementSchema(
            @Schema(name = JsonLdKeywords.TYPE, example = ContractAgreement.CONTRACT_AGREEMENT_TYPE)
            String ldType,
            @Schema(name = JsonLdKeywords.ID)
            String id,
            String providerId,
            String consumerId,
            long contractSigningDate,
            String assetId,
            ManagementApiSchema.PolicySchema policy,
            boolean isRetired,
            long retiredAt,
            String retiredReason
    ) {
        public static final String ENHANCED_CONTRACT_AGREEMENT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/ContractAgreement",
                    "@id": "negotiation-id",
                    "providerId": "provider-id",
                    "consumerId": "consumer-id",
                    "assetId": "asset-id",
                    "contractSigningDate": 1688465655,
                    "policy": {
                        "@context": "http://www.w3.org/ns/odrl.jsonld",
                        "@type": "Set",
                        "@id": "offer-id",
                        "permission": [{
                            "target": "asset-id",
                            "action": "display"
                        }]
                    },
                    "isRetired": true,
                    "retiredAt": 1788465655,
                    "retiredReason": "a good reason"
                }
                """;
    }
}
