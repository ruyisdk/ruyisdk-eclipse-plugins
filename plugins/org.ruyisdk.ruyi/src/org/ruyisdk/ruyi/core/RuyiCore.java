package org.ruyisdk.ruyi.core;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
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
        check(false, 2000); // 延迟2秒检测以避免影响IDE启动性能
    }

    /**
     * Runs manual check.
     */
    public static void runManualCheck() {
        check(true, 0);
    }

    private static void check(boolean isManual, int delayMillis) {
        final var checkType = isManual ? "Manual" : "Background";

        if (isChecking.compareAndSet(false, true)) {
            LOGGER.logInfo(String.format("Ruyi environment check starting (%s)", checkType));

            final var checkJob = Job.create("Ruyi Environment Check", monitor -> {
                try {
                    final var result = CheckRuyiJob.runCheck(monitor);
                    handleCheckResult(result, isManual);
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return Status.error("Ruyi environment check failed", e);
                } finally {
                    LOGGER.logInfo(
                            String.format("Ruyi environment check finished (%s)", checkType));
                    isChecking.set(false);
                }
            });

            if (isManual) {
                checkJob.setUser(true);
            }

            checkJob.schedule(delayMillis);
        } else {
            LOGGER.logInfo("Ruyi environment check is already running");
        }
    }

    private static void handleCheckResult(CheckResult result, boolean isManual) {
        final var action = result.getAction();
        LOGGER.logInfo(String.format("Ruyi environment check result: action: %s, message: %s",
                action, result.getMessage()));

        Display.getDefault().asyncExec(() -> {
            switch (action) {
                case INSTALL:
                    RuyiInstallWizard.openForInstall(result.getLatestVersion());
                    break;

                case UPGRADE:
                    RuyiInstallWizard.openForUpgrade(result.getCurrentVersion(),
                            result.getLatestVersion());
                    break;

                case NOTHING:
                    if (isManual) {
                        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                                "Ruyi Environment Check", "Ruyi environment is up to date.");
                    }
                    break;

                default:
                    LOGGER.logError("Unknown check result action", null);
                    break;
            }
        });
    }
}
