package org.ruyisdk.ruyi.core;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.ruyisdk.core.ruyi.model.CheckResult;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.jobs.CheckRuyiJob;
import org.ruyisdk.ruyi.services.RuyiProperties;
import org.ruyisdk.ruyi.ui.RuyiInstallWizard;

/**
 * Ruyi核心控制类.
 */
public class RuyiCore {
    private static final PluginLogger LOGGER = Activator.getLogger();

    private volatile boolean isChecking;

    /**
     * Starts background environment check.
     */
    public void startBackgroundCheck() {
        boolean autocheck = autoCheckAtStartup();
        LOGGER.logInfo("RuyiAutoCheck set :" + autocheck);
        if (isChecking || !autocheck) {
            return;
        }

        isChecking = true;
        Job.create("Ruyi Environment Check", monitor -> {
            try {
                CheckResult result = new CheckRuyiJob().runCheck(monitor);
                handleCheckResult(result);
                return Status.OK_STATUS;
            } catch (Exception e) {
                return Status.error("Automatic environment check failed", e);
            } finally {
                isChecking = false;
            }
        }).schedule(2000); // 延迟2秒启动
    }

    /**
     * Starts background jobs.
     */
    public void startBackgroundJobs() {
        // 自定义后台任务
    }

    /**
     * Shuts down core services.
     */
    public void shutdown() {
        // 自定义清理工作
        LOGGER.logInfo("RuyiCore services stopped successfully");
    }

    /**
     * Runs manual check.
     */
    public void runManualCheck() {
        Job.create("Manual Ruyi Check", monitor -> {
            try {
                CheckResult result = new CheckRuyiJob().runCheck(monitor);
                handleCheckResult(result);
                return Status.OK_STATUS;
            } catch (Exception e) {
                return Status.error("Manual check failed", e);
            }
        }).schedule();
    }

    private void handleCheckResult(CheckResult result) {
        Display.getDefault().asyncExec(() -> {
            switch (result.getAction()) {
                case INSTALL:
                    RuyiInstallWizard.openForInstall();
                    break;

                case UPGRADE:
                    RuyiInstallWizard.openForUpgrade(result.getCurrentVersion(),
                            result.getLatestVersion());
                    break;

                case NOTHING:
                    LOGGER.logInfo(result.getMessage());
                    break;

                default:
                    LOGGER.logError("Unknown check result action", null);
                    break;
            }
        });
    }

    private boolean autoCheckAtStartup() {
        return RuyiProperties.isAutomaticDetectionEnabled();
        // IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        // return prefs.getBoolean(RuyiPreferenceConstants.P_CHECK_ON_STARTUP) &&
        // !prefs.getBoolean(RuyiPreferenceConstants.P_SKIP_VERSION_CHECK);
    }
}
