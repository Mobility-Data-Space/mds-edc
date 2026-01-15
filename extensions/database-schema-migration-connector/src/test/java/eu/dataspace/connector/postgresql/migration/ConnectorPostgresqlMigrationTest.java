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

package eu.dataspace.connector.postgresql.migration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.postgresql.migration.AccessTokenDataPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.AgreementBpnsPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.AgreementRetirementPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.AssetPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.BusinessGroupPostgresMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.ContractDefinitionPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.ContractNegotiationPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.DataPlaneInstancePostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.DataPlanePostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.EdrIndexPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.FederatedCatalogCacheMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.JtiValidationPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.PolicyMonitorPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.PolicyPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.TransferProcessPostgresqlMigrationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PGobject;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flywaydb.core.api.CoreMigrationType.BASELINE;
import static org.flywaydb.core.api.CoreMigrationType.SQL;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(DependencyInjectionExtension.class)
public class ConnectorPostgresqlMigrationTest {

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
    void shouldThrowException_whenParticipantContextIdNotSet(ObjectFactory objectFactory, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        )));

        var extension = objectFactory.constructInstance(ConnectorPostgresqlMigration.class);

        assertThatThrownBy(() -> extension.initialize(context))
                .hasMessageContaining("edc.participant.context.id");
    }

    @Test
    void shouldRunOnEmptyDatabase(ObjectFactory objectFactory, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.participant.context.id", UUID.randomUUID().toString(),
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        )));
        var newMigrations = objectFactory.constructInstance(ConnectorPostgresqlMigration.class);
        newMigrations.initialize(context);
        newMigrations.prepare();

        try (var connection = createDataSource().getConnection()) {
            var callableStatement = connection.prepareCall("select * from flyway_schema_history;");
            callableStatement.execute();
            var resultSet = callableStatement.getResultSet();
            resultSet.next();
            assertThat(resultSet.getString("version")).isEqualTo("1.0.0");
            assertThat(resultSet.getString("type")).isEqualTo(SQL.toString());
            assertThat(testMigrationHasBeenApplied(connection)).isEqualTo(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Lease {
        @Test
        void shouldRun110whenLeaseEntryAlreadyExists(ObjectFactory objectFactory, ServiceExtensionContext context) {
            var participantContextId = UUID.randomUUID().toString();
            migrateTo(objectFactory, context, "1.0.0", participantContextId);

            try (var connection = createDataSource().getConnection()) {
                var callableStatement = connection.prepareStatement("insert into edc_lease(leased_by, lease_duration, lease_id) values (?, ?, ?);");
                callableStatement.setString(1, UUID.randomUUID().toString());
                callableStatement.setLong(2, 60000L);
                callableStatement.setString(3, UUID.randomUUID().toString());
                callableStatement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThatNoException().isThrownBy(() -> migrateTo(objectFactory, context, "latest", participantContextId));
        }
    }

    @Nested
    class ParticipantContextId {
        @Test
        void shouldAddParticipantContextId_withConfiguredValue(ObjectFactory objectFactory, ServiceExtensionContext context) {
            var participantContextId = UUID.randomUUID().toString();
            migrateTo(objectFactory, context, "1.4.0", participantContextId);

            try (var connection = createDataSource().getConnection()) {
                var callableStatement = connection.prepareStatement("insert into edc_asset(asset_id, participant_context_id) values (?, ?);");
                callableStatement.setString(1, "asset-id");
                callableStatement.setString(2, null);
                callableStatement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            migrateTo(objectFactory, context, "1.5.0", participantContextId);

            try (var connection = createDataSource().getConnection()) {
                var callableStatement = connection.prepareCall("select participant_context_id from edc_asset;");
                callableStatement.execute();
                var resultSet = callableStatement.getResultSet();
                resultSet.next();
                assertThat(resultSet.getString(1)).isEqualTo(participantContextId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void shouldAddParticipantContextId_withConfiguredValue_onJsonTable(ObjectFactory objectFactory, ServiceExtensionContext context) {
            var participantContextId = UUID.randomUUID().toString();
            migrateTo(objectFactory, context, "1.4.0", participantContextId);

            try (var connection = createDataSource().getConnection()) {
                var callableStatement = connection.prepareStatement("insert into edc_data_plane_instance(id, data) values (?, ?);");
                callableStatement.setString(1, "data-plane-id");
                var jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue("{\"participantContextId\":null}");
                callableStatement.setObject(2, jsonObject);
                callableStatement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            migrateTo(objectFactory, context, "1.5.0", participantContextId);

            try (var connection = createDataSource().getConnection()) {
                var callableStatement = connection.prepareCall("select data->>'participantContextId' from edc_data_plane_instance;");
                callableStatement.execute();
                var resultSet = callableStatement.getResultSet();
                resultSet.next();
                assertThat(resultSet.getString(1)).isEqualTo(participantContextId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Test
    @Deprecated(since = "1.0.0") // TODO: also migration to remove the spurious flyway_schema_history_* tables can be added.
    void shouldUseMergedMigrationAsBaseline_whenSchemaAlreadyBuilt(ObjectFactory objectFactory, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.participant.context.id", UUID.randomUUID().toString(),
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        )));
        List<ServiceExtension> currentMigrations = List.of(
                objectFactory.constructInstance(AssetPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AssetPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(ContractDefinitionPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(ContractNegotiationPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(DataPlanePostgresqlMigrationExtension.class),
                objectFactory.constructInstance(EdrIndexPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(FederatedCatalogCacheMigrationExtension.class),
                objectFactory.constructInstance(FederatedCatalogCacheMigrationExtension.class),
                objectFactory.constructInstance(JtiValidationPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(PolicyMonitorPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(PolicyPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(TransferProcessPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AgreementBpnsPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AgreementRetirementPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(BusinessGroupPostgresMigrationExtension.class),
                objectFactory.constructInstance(AccessTokenDataPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(DataPlaneInstancePostgresqlMigrationExtension.class)
        );
        currentMigrations.forEach(e -> e.initialize(context));
        currentMigrations.forEach(ServiceExtension::prepare);

        var newMigrations = objectFactory.constructInstance(ConnectorPostgresqlMigration.class);
        newMigrations.initialize(context);
        newMigrations.prepare();

        try (var connection = createDataSource().getConnection()) {
            var callableStatement = connection.prepareCall("select * from flyway_schema_history;");
            callableStatement.execute();
            var resultSet = callableStatement.getResultSet();
            resultSet.next();
            assertThat(resultSet.getString("version")).isEqualTo("1.0.0");
            assertThat(resultSet.getString("type")).isEqualTo(BASELINE.toString());
            assertThat(testMigrationHasBeenApplied(connection)).isEqualTo(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void migrateTo(ObjectFactory objectFactory, ServiceExtensionContext context, String target, String participantContextId) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.participant.context.id", participantContextId,
                "edc.postgresql.migration.target", target,
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        )));
        var extension = objectFactory.constructInstance(ConnectorPostgresqlMigration.class);
        extension.initialize(context);
        extension.prepare();
    }

    private DataSource createDataSource() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgresql.getJdbcUrl());
        dataSource.setUser(postgresql.getUsername());
        dataSource.setPassword(postgresql.getPassword());
        return dataSource;
    }

    private boolean testMigrationHasBeenApplied(Connection connection) throws SQLException {
        return connection.prepareCall("select dummy_column from edc_policydefinitions;").execute();
    }
}
