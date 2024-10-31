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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.compatibility.tests.fixtures;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class RemoteParticipant extends BaseParticipant {

    private static final String API_KEY = "password";

    public Map<String, String> controlPlaneEnv() {
        return new HashMap<>() {
            {
                put("MY_EDC_PARTICIPANT_ID", id);
                put("MY_EDC_TITLE", id);
                put("MY_EDC_DESCRIPTION", "any");
                put("MY_EDC_NETWORK_TYPE", "unit-test");
                put("MY_EDC_CURATOR_URL", "any");
                put("MY_EDC_CURATOR_NAME", "Example GmbH");
                put("MY_EDC_JDBC_URL", "any");
                put("MY_EDC_JDBC_USER", "any");
                put("MY_EDC_JDBC_PASSWORD", "any");
                put("MY_EDC_MAINTAINER_URL", "https://sovity.de");
                put("MY_EDC_MAINTAINER_NAME", "sovity GmbH");

                put("EDC_PARTICIPANT_ID", id);
                put("EDC_COMPONENT_ID", id);
                put("EDC_API_AUTH_KEY", API_KEY);
                put("WEB_HTTP_PORT", String.valueOf(controlPlaneDefault.getPort()));
                put("WEB_HTTP_PATH", "/api");
                put("WEB_HTTP_PROTOCOL_PORT", String.valueOf(protocolEndpoint.getUrl().getPort()));
                put("WEB_HTTP_PROTOCOL_PATH", protocolEndpoint.getUrl().getPath());
                put("WEB_HTTP_MANAGEMENT_PORT", String.valueOf(managementEndpoint.getUrl().getPort()));
                put("WEB_HTTP_MANAGEMENT_PATH", managementEndpoint.getUrl().getPath());
                put("WEB_HTTP_VERSION_PORT", String.valueOf(controlPlaneVersion.getPort()));
                put("WEB_HTTP_VERSION_PATH", controlPlaneVersion.getPath());
                put("WEB_HTTP_CONTROL_PORT", String.valueOf(controlPlaneControl.getPort()));
                put("WEB_HTTP_CONTROL_PATH", controlPlaneControl.getPath());
                put("EDC_DSP_CALLBACK_ADDRESS", protocolEndpoint.getUrl().toString());
                put("EDC_DATASOURCE_DEFAULT_URL", "jdbc:postgresql://localhost:5432/%s".formatted(getId()));
                put("EDC_DATASOURCE_DEFAULT_USER", "postgres");
                put("EDC_DATASOURCE_DEFAULT_PASSWORD", "password");
                put("EDC_SQL_SCHEMA_AUTOCREATE", "true");
                put("EDC_KEYSTORE", "keystore.p12");
                put("EDC_KEYSTORE_PASSWORD", "123456");
                put("EDC_OAUTH_TOKEN_URL", "any");
                put("EDC_OAUTH_CERTIFICATE_ALIAS", "private-key");
                put("EDC_OAUTH_PRIVATE_KEY_ALIAS", "private-key");
                put("EDC_OAUTH_PROVIDER_JWKS_URL", "http://any");
                put("EDC_LOGGINGHOUSE_EXTENSION_URL", "http://localhost:9999"); // TODO: random address
                put("EDC_DATASOURCE_LOGGINGHOUSE_URL", "jdbc:postgresql://localhost:5432/%s".formatted(getId()));
                put("EDC_DATASOURCE_LOGGINGHOUSE_USER", "postgres");
                put("EDC_DATASOURCE_LOGGINGHOUSE_PASSWORD", "password");

//                put("EDC_TRANSFER_PROXY_TOKEN_SIGNER_PRIVATEKEY_ALIAS", "private-key");
//                put("EDC_TRANSFER_PROXY_TOKEN_VERIFIER_PUBLICKEY_ALIAS", "public-key");
                put("EDC_DPF_SELECTOR_URL", controlPlaneControl + "/v1/dataplanes");
                put("TESTING_EDC_VAULTS_1_KEY", "private-key");
                put("TESTING_EDC_VAULTS_1_VALUE", getPrivateKey());
                put("TESTING_EDC_VAULTS_2_KEY", "public-key");
                put("TESTING_EDC_VAULTS_2_VALUE", getPublicKey());
            }
        };
    }

    public static class Builder extends BaseParticipant.Builder<RemoteParticipant, Builder> {

        protected Builder() {
            super(new RemoteParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }
        
        @Override
        public RemoteParticipant build() {
            var headers = Map.of("x-api-key", API_KEY);
            super.managementEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/management"), headers));
            super.protocolEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/protocol")));
            super.build();
            return participant;
        }
    }
}
