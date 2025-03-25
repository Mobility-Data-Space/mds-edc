package io.nexyo.edp.extensions.utils;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Utility class for logging.
 */
public class LoggingUtils {

    private static Monitor logger;

    /**
     * Private constructor to prevent instantiation.
     */
    private LoggingUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Sets the logger.
     *
     *
     * @param monitor the monitor
     */
    public static synchronized void setLogger(Monitor monitor) {
        logger = monitor;
    }

    /**
     * Gets the logger.
     *
     *
     * @return the logger
     */
    public static synchronized Monitor getLogger() {
        if (logger == null) {
            throw new IllegalStateException("Logger not initialized. Call setLogger() first.");
        }
        return logger;
    }
}
