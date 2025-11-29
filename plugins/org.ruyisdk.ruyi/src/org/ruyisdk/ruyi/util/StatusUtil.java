package org.ruyisdk.ruyi.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.ruyisdk.ruyi.Activator;

/**
 * 状态通知工具类.
 */
public class StatusUtil {

    /**
     * Shows an info dialog.
     *
     * @param message the message
     */
    public static void showInfo(String message) {
        Display.getDefault().asyncExec(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                        "Information", message));
    }

    /**
     * Shows an error dialog.
     *
     * @param message the message
     */
    public static void showError(String message) {
        Display.getDefault().asyncExec(
                        () -> MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message));
    }

    /**
     * Creates an error status.
     *
     * @param message the message
     * @param ex the exception
     * @return error status
     */
    public static IStatus createErrorStatus(String message, Throwable ex) {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, ex);
    }

    /**
     * Creates an info status.
     *
     * @param message the message
     * @return info status
     */
    public static IStatus createInfoStatus(String message) {
        return new Status(IStatus.INFO, Activator.PLUGIN_ID, message);
    }

    /**
     * Logs an error and shows a dialog.
     *
     * @param message the message
     * @param ex the exception
     */
    public static void logAndShow(String message, Throwable ex) {
        Activator.getDefault().getLogger().logError(message, ex);
        showError(message + ": " + ex.getMessage());
    }
}
