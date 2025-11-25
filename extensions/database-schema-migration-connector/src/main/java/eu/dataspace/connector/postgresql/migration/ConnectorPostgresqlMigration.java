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

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.flywaydb.core.Flyway;

import static eu.dataspace.connector.postgresql.migration.ConnectorPostgresqlMigration.NAME;
import static eu.dataspace.connector.postgresql.migration.DatabaseMigrationConfiguration.DEPRECATED_MIGRATION_SCHEMA_KEY;
import static eu.dataspace.connector.postgresql.migration.DatabaseMigrationConfiguration.MIGRATION_SCHEMA_KEY;
import static org.flywaydb.core.api.MigrationVersion.LATEST;

@Extension(NAME)
public class ConnectorPostgresqlMigration implements ServiceExtension {

    public static final String NAME = "Connector Postgresql Schema Migration";

    @Configuration
    private DatabaseMigrationConfiguration configuration;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        if (!configuration.enabled()) {
            monitor.info("Migration for connector disabled");
            return;
        }

        var schema = configuration.deprecatedSchema();
        if (schema != null) {
            monitor.warning("The setting '%s' has been deprecated, please replace it with '%s' as soon as possible"
                    .formatted(DEPRECATED_MIGRATION_SCHEMA_KEY, MIGRATION_SCHEMA_KEY));
        } else {
            schema = configuration.schema();
        }

        var dataSource = configuration.getDataSource();

        var flyway = Flyway.configure()
                .baselineVersion("1.0.0")
                .baselineOnMigrate(true)
                .failOnMissingLocations(true)
                .dataSource(dataSource)
                .table("flyway_schema_history")
                .locations("classpath:migrations/connector")
                .defaultSchema(schema)
                .target(LATEST)
                .load();

        var migrateResult = flyway.migrate();

        if (!migrateResult.success) {
            throw new EdcPersistenceException(
                    "Migrating connector failed: %s".formatted(String.join(", ", migrateResult.warnings))
            );
        }
    }

}
