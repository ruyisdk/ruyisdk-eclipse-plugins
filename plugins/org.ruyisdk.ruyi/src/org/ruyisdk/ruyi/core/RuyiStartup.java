package org.ruyisdk.ruyi.core;

import org.eclipse.ui.IStartup;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.RuyiProperties;

/**
 * Eclipse启动时自动执行的检测逻辑.
 */
public class RuyiStartup implements IStartup {
    private static final PluginLogger LOGGER = Activator.getLogger();

    @Override
    public void earlyStartup() {
        {
            final var autoCheck = RuyiProperties.isAutomaticDetectionEnabled();
            LOGGER.logInfo("RuyiAutoCheck set: " + autoCheck);
            if (autoCheck) {
                RuyiCore.startBackgroundCheck();
            }
        }
    }
}
