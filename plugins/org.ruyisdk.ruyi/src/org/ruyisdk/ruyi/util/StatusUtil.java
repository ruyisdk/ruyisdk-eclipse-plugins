package org.ruyisdk.ruyi.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.ruyisdk.ruyi.Activator;

/**
 * 状态通知工具类
 */
public class StatusUtil {

    public static void showInfo(String message) {
        Display.getDefault().asyncExec(() -> MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                        "Information", message));
    }

    public static void showError(String message) {
        Display.getDefault().asyncExec(
                        () -> MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", message));
    }

    public static IStatus createErrorStatus(String message, Throwable ex) {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, ex);
    }

    public static IStatus createInfoStatus(String message) {
        return new Status(IStatus.INFO, Activator.PLUGIN_ID, message);
    }

    public static void logAndShow(String message, Throwable ex) {
        Activator.getDefault().getLogger().logError(message, ex);
        showError(message + ": " + ex.getMessage());
    }
}
