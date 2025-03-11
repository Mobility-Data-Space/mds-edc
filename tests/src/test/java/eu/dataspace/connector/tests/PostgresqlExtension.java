package eu.dataspace.connector.tests;

import org.eclipse.edc.util.io.Ports;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * JUnit extension that permits to spin up a PostgresSQL container
 */
public class PostgresqlExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String POSTGRES_IMAGE_NAME = "postgres:16.4";
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";
    private static final String DB_SCHEMA_NAME = "test_schema";

    private final PostgreSQLContainer<?> postgreSqlContainer;
    private final String[] databases;
    private final int exposedPort;

    public PostgresqlExtension(String... databases) {
        this.databases = databases;
        exposedPort = Ports.getFreePort();
        this.postgreSqlContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
                .withUsername(USER)
                .withPassword(PASSWORD);
        postgreSqlContainer.setPortBindings(List.of("%d:5432".formatted(exposedPort)));
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSqlContainer.start();
        postgreSqlContainer.waitingFor(Wait.forHealthcheck());
        this.createDatabases();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgreSqlContainer.stop();
        postgreSqlContainer.close();
    }

    public Config getConfig(String databaseName) {
        var jdbcUrl = baseJdbcUrl() + databaseName.toLowerCase() + "?currentSchema=" + DB_SCHEMA_NAME;

        var settings = Map.ofEntries(
                entry("edc.datasource.default.url", jdbcUrl),
                entry("edc.datasource.default.user", USER),
                entry("edc.datasource.default.password", PASSWORD),
                entry("org.eclipse.tractusx.edc.postgresql.migration.schema", DB_SCHEMA_NAME)
        );
        return ConfigFactory.fromMap(settings);
    }

    private void createDatabases() {
        try (var connection = DriverManager.getConnection(baseJdbcUrl() + "postgres", postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword())) {
            var command = stream(databases).map("create database %s;"::formatted).collect(joining("; "));
            connection.createStatement().execute(command);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String baseJdbcUrl() {
        var url = format("jdbc:postgresql://%s:%s/", postgreSqlContainer.getHost(), exposedPort);
        return url;
    }
}
