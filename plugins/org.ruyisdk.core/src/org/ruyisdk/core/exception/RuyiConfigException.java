package org.ruyisdk.core.exception;

import java.io.IOException;

/**
 * Exception thrown when reading or writing configuration or properties files fails.
 */
public class RuyiConfigException extends PluginException {
    private static final long serialVersionUID = 1L;

    private RuyiConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Failed to save a configuration or properties file. */
    public static RuyiConfigException saveFailed(String target, IOException cause) {
        return new RuyiConfigException("Failed to save " + target, cause);
    }

    /** Failed to delete a file or directory. */
    public static RuyiConfigException deleteFailed(String path, IOException cause) {
        return new RuyiConfigException("Failed to delete: " + path, cause);
    }
}
