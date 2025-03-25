package io.nexyo.edp.extensions.utils;

import org.eclipse.edc.boot.config.ConfigurationLoader;
import org.eclipse.edc.boot.config.EnvironmentVariables;
import org.eclipse.edc.boot.config.SystemProperties;
import org.eclipse.edc.boot.system.ServiceLocatorImpl;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.Config;

/**
 * Utility class for loading the configuration.
 */
public class ConfigurationUtils {

    private static Config config;
    public static final String EDR_PROPERTY_EDPS_BASE_URL_KEY = "https://w3id.org/edc/v0.0.1/ns/endpoint";
    public static final String EDR_PROPERTY_EDPS_AUTH_KEY = "https://w3id.org/edc/v0.0.1/ns/authorization";

    /**
     * Private constructor to prevent instantiation.
     */
    private ConfigurationUtils() {

    }

    /**
     * Loads the configuration.
     */
    public static synchronized void loadConfig() {
        var configurationLoader = new ConfigurationLoader(
                new ServiceLocatorImpl(),
                EnvironmentVariables.ofDefault(),
                SystemProperties.ofDefault());

        var logger = LoggingUtils.getLogger();
        config = configurationLoader.loadConfiguration(logger);
    }

    /**
     * Gets the configuration.
     *
     *
     * @return the configuration
     */
    public static synchronized Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    /**
     * Reads a string property from the configuration.
     *
     *
     * @param key          the key
     * @param propertyName the property name
     * @return the property value
     */
    public static String readStringProperty(String key, String propertyName) {
        if (config == null) {
            loadConfig();
        }
        if (key == null || propertyName == null) {
            throw new EdcException("Key and propertyName cannot be null");
        }

        String conf = "";
        try {
            conf = config.getConfig(key).getString(propertyName);
        } catch (EdcException e) {
            // Log the exception (you may want to use a proper logging framework)
            var logger = LoggingUtils.getLogger();
            logger.severe("Configuration not found for key: " + key + ", property: " + propertyName);
        }
        return conf;
    }

}
