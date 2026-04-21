package org.ruyisdk.ruyi.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.ruyisdk.core.ruyi.model.CheckResult;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.RuyiApi;
import org.ruyisdk.ruyi.services.RuyiCliVersionSupport;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

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
        monitor.beginTask("Checking Ruyi environment", 2);

        try {
            monitor.subTask("Detecting current version");
            final var current = getInstalledVersion();
            monitor.worked(1);

            monitor.subTask("Checking latest version");
            final var latest = getLatestRelease();
            monitor.worked(1);

            if (current == null) {
                return CheckResult.needInstall(latest);
            }

            if (latest != null) {
                if (!RuyiCliVersionSupport.isSupportedVersion(current)
                        && current.compareTo(latest) < 0) {
                    return CheckResult.needUpgrade(current, latest);
                }
            }

            return CheckResult.ok();
        } finally {
            monitor.done();
        }
    }

    private RuyiVersion getInstalledVersion() {
        final var installDir = RuyiFileUtils.findInstallPathWithRuyi();
        if (installDir == null || installDir.isBlank()) {
            return null;
        }
        final var version = RuyiCliVersionSupport.getInstalledVersion(installDir);
        LOGGER.logInfo("Installed Ruyi version: " + version);
        return version;
    }

    private RuyiVersion getLatestRelease() {
        final var archSuffix = SystemInfo.detectArchitecture().getSuffix();
        final var info = RuyiApi.getLatestRelease(archSuffix);
        final var latest = info.getVersion();
        LOGGER.logInfo("Latest Ruyi version available: " + latest);
        return latest;
    }
}
