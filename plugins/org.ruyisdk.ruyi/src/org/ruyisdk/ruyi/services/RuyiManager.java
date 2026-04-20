package org.ruyisdk.ruyi.services;

import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Manager for Ruyi operations.
 */
public class RuyiManager {

    /**
     * Checks if Ruyi is installed.
     *
     * @return true if installed
     */
    public static boolean isRuyiInstalled() {
        return !RuyiFileUtils.findInstallPathWithRuyi().isEmpty();
    }

    /**
     * Gets installed Ruyi version.
     *
     * @return installed version or null
     */
    public static RuyiVersion getInstalledVersion() {
        return RuyiCli.getInstalledVersion();
    }

    /**
     * Gets latest Ruyi version.
     *
     * @return latest version or null
     */
    public static RuyiVersion getLatestVersion() {
        final var archSuffix = SystemInfo.detectArchitecture().getSuffix();
        final var info = RuyiApi.getLatestRelease(archSuffix);
        return info.getVersion();
    }
}
