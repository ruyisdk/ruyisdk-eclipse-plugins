package org.ruyisdk.ruyi.core;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.ruyisdk.core.ruyi.model.CheckResult;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.jobs.CheckRuyiJob;
import org.ruyisdk.ruyi.ui.RuyiInstallWizard;

/**
 * Ruyi核心控制类.
 */
public class RuyiCore {
    private static final PluginLogger LOGGER = Activator.getLogger();
    private static final AtomicBoolean isChecking = new AtomicBoolean();

    private RuyiCore() {

    }

    /**
     * Starts background environment check.
     */
    public static void startBackgroundCheck() {
        check(2000); // 延迟2秒检测以避免影响IDE启动性能
    }

    /**
     * Runs manual check.
     */
    public static void runManualCheck() {
        check(0);
    }

    private static void check(int delayMillis) {
        if (isChecking.compareAndSet(false, true)) {
            Job.create("Ruyi Environment Check", monitor -> {
                try {
                    CheckResult result = CheckRuyiJob.runCheck(monitor);
                    handleCheckResult(result);
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return Status.error("Ruyi environment check failed", e);
                } finally {
                    isChecking.set(false);
                }
            }).schedule(delayMillis);
        }
    }

    private static void handleCheckResult(CheckResult result) {
        Display.getDefault().asyncExec(() -> {
            switch (result.getAction()) {
                case INSTALL:
                    RuyiInstallWizard.openForInstall(result.getLatestVersion());
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
}
