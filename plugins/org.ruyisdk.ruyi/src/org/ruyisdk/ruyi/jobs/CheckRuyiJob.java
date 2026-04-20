package org.ruyisdk.ruyi.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.ruyisdk.core.ruyi.model.CheckResult;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.RuyiManager;

/**
 * Job for checking Ruyi environment.
 */
public class CheckRuyiJob {
    private static final PluginLogger LOGGER = Activator.getLogger();

    /**
     * Runs Ruyi environment check.
     *
     * @param monitor progress monitor
     * @return check result
     */
    public CheckResult runCheck(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Checking Ruyi environment", 3);

            // Step 1: Check if installed
            // 步骤1: 检查是否安装
            monitor.subTask("Checking installation");
            if (!isInstalled()) {
                return CheckResult.needInstall("Ruyi is not installed");
            }
            monitor.worked(1);

            // 步骤2: 获取当前版本
            monitor.subTask("Detecting current version");
            RuyiVersion current = getInstalledVersion();
            monitor.worked(1);

            // 步骤3: 检查新版本
            monitor.subTask("Checking latest version");
            RuyiVersion latest = getLatestRelease();

            if (latest != null && current.compareTo(latest) < 0) {
                return CheckResult.needUpgrade(current, latest,
                        String.format("New version available: %s (current: %s)", latest.toString(),
                                current.toString()));
            }

            return CheckResult.ok();
        } finally {
            monitor.done();
        }
    }

    private boolean isInstalled() {
        final var installed = RuyiManager.isRuyiInstalled();
        LOGGER.logInfo("Ruyi is installed ? " + installed);
        return installed;
    }

    private RuyiVersion getInstalledVersion() {
        final var version = RuyiManager.getInstalledVersion();
        LOGGER.logInfo("Installed Ruyi version: " + version);
        return version;
    }

    private RuyiVersion getLatestRelease() {
        final var latest = RuyiManager.getLatestVersion();
        LOGGER.logInfo("Latest Ruyi version available: " + latest);
        return latest;
    }
}
