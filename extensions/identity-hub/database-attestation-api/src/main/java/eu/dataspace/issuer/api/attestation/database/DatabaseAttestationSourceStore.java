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

package eu.dataspace.issuer.api.attestation.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.statement.SqlExecuteStatement;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.SQLException;

public class DatabaseAttestationSourceStore extends AbstractSqlStore {

    public DatabaseAttestationSourceStore(String dataSourceName, ObjectMapper objectMapper, DataSourceRegistry dataSourceRegistry, QueryExecutor queryExecutor, TransactionContext transactionContext) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
    }

    public StoreResult<Void> create(HolderAttestation attestation) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {

                var query = SqlExecuteStatement.newInstance("::json")
                        .column("holder_id")
                        .column("participant_name")
                        .column("membership_type")
                        .column("membership_start_date")
                        .insertInto("membership_attestation");
                queryExecutor.execute(connection, query, attestation.holderId(), attestation.participantName(), attestation.membershipType(), attestation.membershipStartDate());
                return StoreResult.success();

            } catch (SQLException e) {
                return StoreResult.generalError(e.getMessage());
            }
        });
    }
}
