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
import org.eclipse.tractusx.edc.postgresql.migration.EdrIndexPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.FederatedCatalogCacheMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.JtiValidationPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.PolicyMonitorPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.PolicyPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.TransferProcessPostgresqlMigrationExtension;
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
    void shouldRunOnEmptyDatabase(ObjectFactory objectFactory, ServiceExtensionContext context) {
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

    @Test
    @Deprecated(since = "1.0.0") // TODO: also migration to remove the spurious flyway_schema_history_* tables can be added.
    void shouldUseMergedMigrationAsBaseline_whenSchemaAlreadyBuilt(ObjectFactory objectFactory, ServiceExtensionContext context) {
        List<ServiceExtension> currentMigrations = List.of(
                objectFactory.constructInstance(AssetPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AssetPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(ContractDefinitionPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(ContractNegotiationPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(DataPlaneInstancePostgresqlMigrationExtension.class),
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
