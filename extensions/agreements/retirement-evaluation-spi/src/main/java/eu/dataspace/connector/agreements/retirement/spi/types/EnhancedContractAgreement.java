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

package eu.dataspace.connector.agreements.retirement.spi.types;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;

public record EnhancedContractAgreement(
    ContractAgreement agreement,
    AgreementsRetirementEntry retirement
) { }
