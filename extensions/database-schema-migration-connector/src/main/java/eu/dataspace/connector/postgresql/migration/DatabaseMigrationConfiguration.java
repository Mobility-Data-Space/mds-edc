/*
 * Copyright (c) 2025 Think-it GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package eu.dataspace.connector.postgresql.migration;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.sql.DriverManagerConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;

import java.util.Properties;
import javax.sql.DataSource;

@Settings
public record DatabaseMigrationConfiguration(

        @Setting(
                key = "edc.postgresql.migration.enabled",
                description = "Enable/disables data-plane schema migration",
                defaultValue = DEFAULT_MIGRATION_ENABLED_TEMPLATE)
        boolean enabled,

        @Setting(
                key = MIGRATION_SCHEMA_KEY,
                description = "Schema used for the migration",
                defaultValue = DEFAULT_MIGRATION_SCHEMA
        )
        String schema,

        @Setting(
                key = MIGRATION_TARGET_VERSION,
                description = "Target version for the migration",
                defaultValue = DEFAULT_TARGET_VERSION
        )
        String target,

        @Deprecated(since = "1.0.0")
        @Setting(
                key = DEPRECATED_MIGRATION_SCHEMA_KEY,
                description = "Deprecated key: Schema used for the migration",
                required = false
        )
        String deprecatedSchema,

        @Setting(
                description = "Configures the participant context id for the single participant runtime",
                key = "edc.participant.context.id",
                required = false
        )
        String participantContextId,

        @Setting(
                key = "edc.datasource.default.url",
                description = "DataSource JDBC url"
        )
        String url,

        @Setting(
                key = "edc.datasource.default.user",
                description = "DataSource JDBC user"
        )
        String user,

        @Setting(
                key = "edc.datasource.default.password",
                description = "DataSource JDBC password"
        )
        String password
) {
    private static final String DEFAULT_MIGRATION_ENABLED_TEMPLATE = "true";
    private static final String DEFAULT_MIGRATION_SCHEMA = "public";
    private static final String DEFAULT_TARGET_VERSION = "latest";
    @Deprecated(since = "1.0.0")
    public static final String DEPRECATED_MIGRATION_SCHEMA_KEY = "org.eclipse.tractusx.edc.postgresql.migration.schema";
    public static final String MIGRATION_SCHEMA_KEY = "edc.postgresql.migration.schema";
    public static final String MIGRATION_TARGET_VERSION = "edc.postgresql.migration.target";

    /**
     * Instance and return DataSource to be passed to Flyway for schema migrations
     *
     * @return the dataSource.
     */
    public DataSource getDataSource() {
        var jdbcProperties = new Properties();
        jdbcProperties.put("user", user);
        jdbcProperties.put("password", password);
        var driverManagerConnectionFactory = new DriverManagerConnectionFactory();
        return new ConnectionFactoryDataSource(driverManagerConnectionFactory, url, jdbcProperties);
    }

}
