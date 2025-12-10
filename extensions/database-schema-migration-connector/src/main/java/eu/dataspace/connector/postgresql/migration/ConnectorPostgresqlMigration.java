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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.flywaydb.core.Flyway;

import java.util.Map;
import java.util.UUID;

import static eu.dataspace.connector.postgresql.migration.ConnectorPostgresqlMigration.NAME;
import static eu.dataspace.connector.postgresql.migration.DatabaseMigrationConfiguration.DEPRECATED_MIGRATION_SCHEMA_KEY;
import static eu.dataspace.connector.postgresql.migration.DatabaseMigrationConfiguration.MIGRATION_SCHEMA_KEY;

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
    public void initialize(ServiceExtensionContext context) {
        if (configuration.enabled() && configuration.participantContextId() == null) {
            throw new EdcException("The participant context id has not been set, it is a mandatory setting now. You can " +
                    "use this UUID generated randomly for you: %s, or you can generate one by yourself. Please note that"
                            .formatted(UUID.randomUUID().toString()) +
                    " once set, it must never change. Depending on how you are configuring the Connector, set it on the " +
                    "`edc.participant.context.id` setting/system property or `EDC_PARTICIPANT_CONTEXT_ID` environment " +
                    "variable, then restart the Connector");
        }
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
                .target(configuration.target())
                .placeholders(Map.of("ParticipantContextId", configuration.participantContextId()))
                .load();

        var migrateResult = flyway.migrate();

        if (!migrateResult.success) {
            throw new EdcPersistenceException(
                    "Migrating connector failed: %s".formatted(String.join(", ", migrateResult.warnings))
            );
        }
    }

}
