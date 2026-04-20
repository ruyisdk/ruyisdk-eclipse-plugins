package org.ruyisdk.ruyi.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ruyisdk.core.exception.PluginException;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.PackageIndexUpdater;

/**
 * Handler for the "Update Package Index" command. Runs {@code ruyi update} with a progress dialog.
 */
public class UpdatePackageIndexHandler extends AbstractHandler {
    private static final PluginLogger LOGGER = Activator.getLogger();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final var shell = HandlerUtil.getActiveShell(event);
        try {
            PackageIndexUpdater.updateWithProgress(shell);
            LOGGER.logInfo("Package index updated successfully");
            if (shell != null) {
                MessageDialog.openInformation(shell, "Update Package Index",
                        "Package index updated successfully.");
            }
            return null;
        } catch (PluginException e) {
            final var msg = "Failed to update the package index";
            LOGGER.logError(msg, e);
            if (shell != null) {
                MessageDialog.openError(shell, msg, String.format("""
                    Unable to update the Ruyi package index.

                    %s""", e.getMessage()));
            }
            throw new ExecutionException(msg, e);
        }
    }
}
