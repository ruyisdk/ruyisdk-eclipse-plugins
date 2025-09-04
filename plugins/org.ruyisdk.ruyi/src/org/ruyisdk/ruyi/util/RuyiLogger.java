package org.ruyisdk.ruyi.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.ruyisdk.ruyi.Activator;

public class RuyiLogger {
    private final ILog eclipseLog;

    public RuyiLogger(ILog eclipseLog) {
        this.eclipseLog = eclipseLog;
    }

    public void logInfo(String message) {
        log(IStatus.INFO, message, null);
    }

    public void logWarning(String message, Throwable exception) {
        log(IStatus.WARNING, message, exception);
    }

    public void logError(String message, Throwable exception) {
        log(IStatus.ERROR, message, exception);
    }

    public void log(int severity, String message, Throwable exception) {
        IStatus status = new Status(severity, Activator.PLUGIN_ID, message, exception);
        eclipseLog.log(status);
    }

    public void log(IStatus status) {
        eclipseLog.log(status);
    }
}
