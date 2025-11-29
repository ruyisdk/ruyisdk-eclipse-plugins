package org.ruyisdk.ruyi.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.ruyisdk.ruyi.Activator;

/**
 * Logger utility for Ruyi plugin.
 */
public class RuyiLogger {
    private final ILog eclipseLog;

    /**
     * Constructs a logger.
     *
     * @param eclipseLog the Eclipse log
     */
    public RuyiLogger(ILog eclipseLog) {
        this.eclipseLog = eclipseLog;
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
        IStatus status = new Status(severity, Activator.PLUGIN_ID, message, exception);
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
