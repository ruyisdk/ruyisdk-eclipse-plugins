package org.ruyisdk.ruyi.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.ruyisdk.core.ruyi.model.CheckResult;
import org.ruyisdk.core.ruyi.model.RuyiReleaseInfo;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.RuyiManager;
import org.ruyisdk.ruyi.util.RuyiLogger;

/**
 * 执行Ruyi环境检测的任务
 */
public class CheckRuyiJob {
    private static final RuyiLogger logger = Activator.getDefault().getLogger();

    public CheckResult runCheck(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Checking Ruyi environment", 3);

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
                return CheckResult.needUpgrade(current, latest, String.format("New version available: %s (current: %s)",
                                latest.toString(), current.toString()));
            }

            return CheckResult.ok();
        } catch (OperationCanceledException e) {
            logger.logInfo("Version check cancelled");
            throw e;
        } catch (Exception e) {
            logger.logError("Version check failed", e);
            throw new RuntimeException("Check failed: " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }

    private boolean isInstalled() {
        System.out.println("Ruyi is installed ? " + RuyiManager.isRuyiInstalled());
        return RuyiManager.isRuyiInstalled();
    }

    private RuyiVersion getInstalledVersion() {
        System.out.println("Ruyi is installed ? " + RuyiManager.isRuyiInstalled());
        return RuyiManager.getInstalledVersion();
    }

    private RuyiVersion getLatestRelease() {
        System.out.println("Ruyi has new version: " + RuyiManager.getLatestVersion());
        return RuyiManager.getLatestVersion();
    }
}
