package org.ruyisdk.ruyi.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.core.RuyiCore;

/**
 * Handler for check installation command.
 */
public class CheckInstallationHandler extends AbstractHandler {
    private static final PluginLogger LOGGER = Activator.getLogger();

    private RuyiCore ruyiCore; // 核心服务

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            LOGGER.logInfo("Manual installation check triggered");
            ruyiCore = new RuyiCore(LOGGER);
            ruyiCore.runManualCheck();
            LOGGER.logInfo("Ruyi activated successfully.");
            return null;
        } catch (Exception e) {
            LOGGER.logError("Failed to execute check installation command", e);
            throw new ExecutionException("Check installation failed", e);
        }
    }
}
