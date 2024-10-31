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

import io.restassured.common.mapper.TypeRef;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class BaseParticipant extends Participant {

    protected static String privateKey = getResourceFileContentAsString("certs/key.pem");
    protected static String publicKey = getResourceFileContentAsString("certs/cert.pem");

    protected final URI controlPlaneDefault = URI.create("http://localhost:" + getFreePort());
    protected final URI controlPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    protected final URI dataPlaneDefault = URI.create("http://localhost:" + getFreePort());
    protected final URI dataPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    protected final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");
    protected final URI controlPlaneVersion = URI.create("http://localhost:" + getFreePort() + "/version");
    protected final URI dataPlaneVersion = URI.create("http://localhost:" + getFreePort() + "/version");

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Pull data from provider using EDR.
     *
     * @param edr           endpoint data reference
     * @param queryParams   query parameters
     * @param bodyAssertion assertion to be verified on the body
     */
    public void pullData(DataAddress edr, Map<String, String> queryParams, ThrowingConsumer<String> bodyAssertion) {
        var data = given()
                .baseUri(edr.getStringProperty("endpoint"))
                .header("Authorization", edr.getStringProperty("authorization"))
                .queryParams(queryParams)
                .when()
                .get()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(data).satisfies(bodyAssertion);
    }

    public void waitForDataPlane() {
        await().atMost(timeout)
                .untilAsserted(() -> {
                    var jp = managementEndpoint.baseRequest()
                            .get("/v3/dataplanes")
                            .then()
                            .statusCode(200)
                            .log().ifValidationFails()
                            .extract().body().jsonPath();

                    var state = jp.getString("state");
                    assertThat(state).isEqualTo("[AVAILABLE]");
                });

    }

    /**
     * Get the EDR from the EDR cache by transfer process id.
     *
     * @param transferProcessId The transfer process id
     * @return The cached {@link DataAddress}
     */
    public DataAddress getEdr(String transferProcessId) {
        var dataAddressRaw = managementEndpoint.baseRequest()
                .contentType(JSON)
                .when()
                .get("/v3/edrs/{id}/dataaddress", transferProcessId)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().body().as(new TypeRef<Map<String, Object>>() {
                });


        var builder = DataAddress.Builder.newInstance();
        dataAddressRaw.forEach(builder::property);
        return builder.build();

    }

    public void awaitTransferToBeInState(String transferProcessId, TransferProcessStates state) {
        await().atMost(timeout).until(
                () -> getTransferProcessState(transferProcessId),
                it -> Objects.equals(it, state.name())
        );
    }

    public static class Builder<P extends BaseParticipant, B extends Participant.Builder<P, B>> extends Participant.Builder<P, B> {

        protected Builder(P participant) {
            super(participant);
        }
        
    }
}
