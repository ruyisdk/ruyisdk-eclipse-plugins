package org.ruyisdk.ruyi.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.DeviceProvisionService;

/**
 * Handler for launching flash device (device provision) workflow.
 */
public class FlashDeviceProvisionHandler extends AbstractHandler {
    private static final PluginLogger LOGGER = Activator.getLogger();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            new DeviceProvisionService().launchProvisionWizard();
            LOGGER.logInfo("Flash device (device provision) command launched in built-in terminal.");
            return null;
        } catch (Exception e) {
            LOGGER.logError("Failed to launch flash device (device provision) command", e);
            final var shell = HandlerUtil.getActiveShell(event);
            if (shell != null) {
                MessageDialog.openError(shell, "Failed to launch flash device (device provision)",
                                "Unable to start built-in terminal for 'ruyi device provision'.\n\n" + e.getMessage());
            }
            throw new ExecutionException("Failed to launch flash device (device provision)", e);
        }
    }
}
