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

package eu.dataspace.wallet.database.schema.migrations;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(DependencyInjectionExtension.class)
public class DatabaseSchemaMigrationsWalletExtensionTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "credential_resource",
            "did_resources",
            "edc_lease",
            "edc_holder_credentialrequest",
            "edc_jti_validation",
            "edc_sts_client",
            "keypair_resource",
            "participant_context",
            "edc_participant_context_config"
    );

    @Container
    private final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:18.1");

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        )));
    }

    @Test
    void shouldRunOnEmptyDatabase(ObjectFactory objectFactory, ServiceExtensionContext context) {
        var extension = objectFactory.constructInstance(DatabaseSchemaMigrationsWalletExtension.class);

        assertThatNoException().isThrownBy(() -> {
            extension.initialize(context);
            extension.prepare();
        });

        try (var connection = createDataSource().getConnection()) {
            for (var table : EXPECTED_TABLES) {
                assertThat(tableExists(connection, table))
                        .as("table %s should exist after Flyway migrations", table)
                        .isTrue();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldCreateParticipantContextConfigTableWithExpectedColumns(ObjectFactory objectFactory, ServiceExtensionContext context) {
        var extension = objectFactory.constructInstance(DatabaseSchemaMigrationsWalletExtension.class);
        extension.initialize(context);
        extension.prepare();

        try (var connection = createDataSource().getConnection()) {
            var statement = connection.prepareStatement(
                    "select column_name from information_schema.columns where table_name = 'edc_participant_context_config'"
            );
            statement.execute();
            var resultSet = statement.getResultSet();
            var columns = new java.util.HashSet<String>();
            while (resultSet.next()) {
                columns.add(resultSet.getString(1));
            }
            assertThat(columns).contains(
                    "participant_context_id",
                    "created_date",
                    "last_modified_date",
                    "entries",
                    "private_entries"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldRecordMigrationsInFlywayHistory(ObjectFactory objectFactory, ServiceExtensionContext context) {
        var extension = objectFactory.constructInstance(DatabaseSchemaMigrationsWalletExtension.class);
        extension.initialize(context);
        extension.prepare();

        try (var connection = createDataSource().getConnection()) {
            var statement = connection.prepareStatement(
                    "select version from flyway_schema_history where success = true order by installed_rank"
            );
            statement.execute();
            var resultSet = statement.getResultSet();
            var versions = new java.util.ArrayList<String>();
            while (resultSet.next()) {
                versions.add(resultSet.getString(1));
            }
            assertThat(versions).contains("0.0.1", "0.1.0", "0.2.0");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldBeIdempotent_whenRunTwice(ObjectFactory objectFactory, ServiceExtensionContext context) {
        var first = objectFactory.constructInstance(DatabaseSchemaMigrationsWalletExtension.class);
        first.initialize(context);
        first.prepare();

        var second = objectFactory.constructInstance(DatabaseSchemaMigrationsWalletExtension.class);
        assertThatNoException().isThrownBy(() -> {
            second.initialize(context);
            second.prepare();
        });
    }

    private DataSource createDataSource() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgresql.getJdbcUrl());
        dataSource.setUser(postgresql.getUsername());
        dataSource.setPassword(postgresql.getPassword());
        return dataSource;
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement(
                "select 1 from information_schema.tables where table_name = ?"
        )) {
            statement.setString(1, table);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
