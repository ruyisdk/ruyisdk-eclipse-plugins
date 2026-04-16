package org.ruyisdk.ruyi.services;

import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;

/**
 * Manager for Ruyi operations.
 */
public class RuyiManager {
    private static final PluginLogger LOGGER = Activator.getLogger();

    /**
     * Checks if Ruyi is installed.
     *
     * @return true if installed
     */
    public static boolean isRuyiInstalled() {
        return getInstalledVersion() != null;
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
        try {
            final var archSuffix = SystemInfo.detectArchitecture().getSuffix();
            final var info = RuyiApi.getLatestRelease(archSuffix);
            final var version = info.getVersion();
            return version;
        } catch (Exception e) {
            LOGGER.logError("Failed to get latest Ruyi version", e);
            return null;
        }
    }
}
