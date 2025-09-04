package org.ruyisdk.ruyi.core;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.ruyisdk.core.ruyi.model.CheckResult;
import org.ruyisdk.ruyi.jobs.CheckRuyiJob;
import org.ruyisdk.ruyi.services.RuyiProperties;
import org.ruyisdk.ruyi.ui.RuyiInstallWizard;
import org.ruyisdk.ruyi.util.RuyiLogger;

/**
 * Ruyi核心控制类
 */
public class RuyiCore {
    private volatile boolean isChecking;
    private final RuyiLogger logger;

    public RuyiCore(RuyiLogger logger) {
        this.logger = logger;
    }

    public void startBackgroundCheck() {
        boolean autocheck = autoCheckAtStartup();
        logger.logInfo("RuyiAutoCheck set :" + autocheck);
        if (isChecking || !autocheck)
            return;

        isChecking = true;
        Job.create("Ruyi Environment Check", monitor -> {
            try {
                CheckResult result = new CheckRuyiJob().runCheck(monitor);
                handleCheckResult(result);
                return Status.OK_STATUS;
            } finally {
                isChecking = false;
            }
        }).schedule(2000); // 延迟2秒启动
    }

    public void startBackgroundJobs() {
        // 自定义后台任务
    }

    public void shutdown() {
        // 自定义清理工作
        logger.logInfo("RuyiCore services stopped successfully");
    }

    public void runManualCheck() {
        Job.create("Manual Ruyi Check", monitor -> {
            CheckResult result = new CheckRuyiJob().runCheck(monitor);
            handleCheckResult(result);
            return Status.OK_STATUS;
        }).schedule();
    }

    private void handleCheckResult(CheckResult result) {
        Display.getDefault().asyncExec(() -> {
            switch (result.getAction()) {
                case INSTALL:
                    // if (confirmAction("Install Ruyi", result.getMessage())) {
                    RuyiInstallWizard.openForInstall();
                    // } else {
                    // StatusUtil.showInfo("You can install Ruyi later from Preferences");
                    // }
                    break;

                case UPGRADE:
                    // if (confirmAction("Upgrade Ruyi", result.getMessage())) {
                    RuyiInstallWizard.openForUpgrade(result.getCurrentVersion(), result.getLatestVersion());
                    // }
                    break;

                case NOTHING:
                    logger.logInfo(result.getMessage());
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

    private boolean confirmAction(String title, String message) {
        return MessageDialog.openQuestion(Display.getDefault().getActiveShell(), title,
                        message + "\n\nWould you like to proceed?");
    }
}
