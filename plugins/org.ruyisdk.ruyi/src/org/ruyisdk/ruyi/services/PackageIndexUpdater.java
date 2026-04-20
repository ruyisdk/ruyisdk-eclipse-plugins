package org.ruyisdk.ruyi.services;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ruyisdk.core.exception.PluginException;
import org.ruyisdk.ruyi.Activator;

/**
 * UI helper that runs {@code ruyi update} inside a {@link ProgressMonitorDialog}.
 */
public final class PackageIndexUpdater {
    private PackageIndexUpdater() {}

    /**
     * Runs {@link RuyiCli#updatePackageIndex()} in a non-cancelable {@link ProgressMonitorDialog}
     * parented at the given shell.
     *
     * @param shell parent shell for the progress dialog
     */
    public static void updateWithProgress(Shell shell) {
        try {
            new ProgressMonitorDialog(shell).run(true, false, monitor -> {
                monitor.beginTask("Updating package index...", IProgressMonitor.UNKNOWN);
                try {
                    RuyiCli.updatePackageIndex();
                } catch (RuntimeException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
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
    }
}
