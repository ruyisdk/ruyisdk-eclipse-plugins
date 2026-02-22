package org.ruyisdk.core.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.ruyisdk.core.Activator;

/**
 * Logger utility.
 */
public class PluginLogger {
    private final ILog eclipseLog;
    private final String pluginId;

    /**
     * Constructs a logger.
     *
     * @param eclipseLog the Eclipse log
     */
    public PluginLogger(ILog eclipseLog) {
        this(eclipseLog, Activator.PLUGIN_ID);
    }

    /**
     * Constructs a logger.
     *
     * @param eclipseLog the Eclipse log
     * @param pluginId the plugin ID used for {@link IStatus} entries
     */
    public PluginLogger(ILog eclipseLog, String pluginId) {
        this.eclipseLog = eclipseLog;
        this.pluginId = pluginId == null ? Activator.PLUGIN_ID : pluginId;
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public void logInfo(String message) {
        log(IStatus.INFO, message, null);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     * @param exception the exception
     */
    public void logWarning(String message, Throwable exception) {
        log(IStatus.WARNING, message, exception);
    }

    /**
     * Logs an error message.
     *
     * @param message the message
     * @param exception the exception
     */
    public void logError(String message, Throwable exception) {
        log(IStatus.ERROR, message, exception);
    }

    /**
     * Logs a message with severity.
     *
     * @param severity the severity level
     * @param message the message
     * @param exception the exception
     */
    public void log(int severity, String message, Throwable exception) {
        IStatus status = new Status(severity, pluginId, message, exception);
        eclipseLog.log(status);
    }

    /**
     * Logs a status.
     *
     * @param status the status
     */
    public void log(IStatus status) {
        eclipseLog.log(status);
    }
}
