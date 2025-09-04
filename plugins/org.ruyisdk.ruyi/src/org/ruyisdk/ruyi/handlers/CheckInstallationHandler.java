package org.ruyisdk.ruyi.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.core.RuyiCore;
import org.ruyisdk.ruyi.jobs.CheckRuyiJob;
import org.ruyisdk.ruyi.util.RuyiLogger;

/**
 * 处理"检查安装"命令
 */
public class CheckInstallationHandler extends AbstractHandler {
    private static final RuyiLogger logger = Activator.getDefault().getLogger();
    private RuyiCore ruyiCore; // 核心服务

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            logger.logInfo("Manual installation check triggered");
            ruyiCore = new RuyiCore(logger);
            ruyiCore.runManualCheck();
            logger.logInfo("Ruyi activated successfully.");
            return null;
        } catch (Exception e) {
            logger.logError("Failed to execute check installation command", e);
            throw new ExecutionException("Check installation failed", e);
        }
    }
}
