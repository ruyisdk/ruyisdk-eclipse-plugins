package org.ruyisdk.ruyi.services;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;

/**
 * UI helper that runs {@code ruyi update} inside a {@link ProgressMonitorDialog}.
 */
public final class PackageIndexUpdater {
    private static final PluginLogger LOGGER = Activator.getLogger();

    private PackageIndexUpdater() {}

    /**
     * Runs {@link RuyiCli#updatePackageIndex()} in a non-cancelable {@link ProgressMonitorDialog}
     * parented at the given shell.
     *
     * @param shell parent shell for the progress dialog
     */
    public static boolean updateWithProgress(Shell shell) {
        try {
            new ProgressMonitorDialog(shell).run(true, false, monitor -> {
                monitor.beginTask("Updating package index...", IProgressMonitor.UNKNOWN);
                try {
                    RuyiCli.updatePackageIndex();
                } finally {
                    monitor.done();
                }
            });

            LOGGER.logInfo("Package index updated successfully");
            if (shell != null) {
                MessageDialog.openInformation(shell, "Update Package Index",
                        "Package index updated successfully.");
            }

            return true;
        } catch (InvocationTargetException e) {
            StatusManager.getManager().handle(
                    Status.error("Failed to update package index.", e.getCause()),
                    StatusManager.LOG | StatusManager.BLOCK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            StatusManager.getManager().handle(
                    new Status(IStatus.CANCEL, Activator.PLUGIN_ID, "Operation was cancelled.", e),
                    StatusManager.LOG | StatusManager.BLOCK);
        }

        return false;
    }
}
