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

package eu.dataspace.issuer.database.schema.migrations;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.DriverManagerConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

import java.util.Properties;

import static org.flywaydb.core.api.MigrationVersion.LATEST;

public class DatabaseSchemaMigrationsIssuerExtension implements ServiceExtension {

    private static final String DEFAULT_SCHEMA = "public";

    private ConnectionFactoryDataSource dataSource;

    @Setting(key = "eu.dataspace.issuer.postgresql.migration.schema", defaultValue = DEFAULT_SCHEMA, description = "Schema on which the migrations will be applied")
    private String schema;

    @Override
    public String name() {
        return "Issuer Database Schema Migrations";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = context.getConfig();
        var datasourceConfig = config.getConfig("edc.datasource.default");

        var jdbcUrl = datasourceConfig.getString("url");
        var jdbcProperties = new Properties();
        jdbcProperties.putAll(datasourceConfig.getRelativeEntries());
        var driverManagerConnectionFactory = new DriverManagerConnectionFactory();
        dataSource = new ConnectionFactoryDataSource(driverManagerConnectionFactory, jdbcUrl, jdbcProperties);
    }

    @Override
    public void prepare() {
        var flyway =
                Flyway.configure()
                        .baselineVersion(MigrationVersion.fromVersion("0.0.0"))
                        .failOnMissingLocations(true)
                        .dataSource(dataSource)
                        .table("flyway_schema_history")
                        .locations("classpath:migrations")
                        .target(LATEST)
                        .defaultSchema(schema)
                        .load();

        flyway.baseline();

        var result = flyway.migrate();
        if (!result.success) {
            throw new EdcException("Migrating database schema failed: %s".formatted(String.join(",", result.warnings)));
        }
    }
}
