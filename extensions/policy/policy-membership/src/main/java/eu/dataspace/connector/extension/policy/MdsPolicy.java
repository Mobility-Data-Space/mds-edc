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

package eu.dataspace.connector.extension.policy;

/**
 * Identifiers of the MDS ODRL profile 2026/1.
 *
 * @see <a href="https://w3id.org/mobility-dataspace/2026/1/policy/">MDS ODRL Profile</a>
 */
public interface MdsPolicy {

    String MDS_POLICY_NAMESPACE = "https://w3id.org/mobility-dataspace/2026/1/policy/";
    String MDS_POLICY_CONTEXT = MDS_POLICY_NAMESPACE + "context.jsonld";

}
