package org.ruyisdk.core.basedir;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for resolving XDG Base Directory Specification compliant paths. Provides methods to
 * locate configuration, cache, data and state directories according to the XDG standard.
 */
public class XdgDirs {

    /**
     * Gets the XDG config directory path. Resolution order: 1. {@code $XDG_CONFIG_HOME/<app-name>} if
     * environment variable is set 2. {@code ~/.config/<app-name>} as fallback
     *
     * @param appName the application name used for subdirectory
     * @return resolved config directory path
     */
    public static Path getConfigDir(String appName) {
        return getXdgDir("XDG_CONFIG_HOME", ".config", appName);
    }

    /**
     * Gets the XDG cache directory path. Resolution order: 1. {@code $XDG_CACHE_HOME/<app-name>} if
     * environment variable is set 2. {@code ~/.cache/<app-name>} as fallback
     *
     * @param appName the application name used for subdirectory
     * @return resolved cache directory path
     */
    public static Path getCacheDir(String appName) {
        return getXdgDir("XDG_CACHE_HOME", ".cache", appName);
    }

    /**
     * Gets the XDG data directory path. Resolution order: 1. {@code $XDG_DATA_HOME/<app-name>} if
     * environment variable is set 2. {@code ~/.local/share/<app-name>} as fallback
     *
     * @param appName the application name used for subdirectory
     * @return resolved data directory path
     */
    public static Path getDataDir(String appName) {
        return getXdgDir("XDG_DATA_HOME", ".local/share", appName);
    }

    /**
     * Gets the XDG state directory path. Resolution order: 1. {@code $XDG_STATE_HOME/<app-name>} if
     * environment variable is set 2. {@code ~/.local/state/<app-name>} as fallback
     *
     * @param appName the application name used for subdirectory
     * @return resolved state directory path
     */
    public static Path getStateDir(String appName) {
        return getXdgDir("XDG_STATE_HOME", ".local/state", appName);
    }

    /**
     * Internal method for resolving XDG directory paths. Follows XDG Base Directory Specification
     * resolution rules: 1. Checks specified environment variable first 2. Falls back to default path
     * under user home if not set
     *
     * @param xdgEnvVar the XDG environment variable name (e.g. "XDG_CONFIG_HOME")
     * @param defaultRelativePath the default path relative to user home (e.g. ".config")
     * @param appName the application name for final subdirectory
     * @return fully resolved directory path
     * @throws NullPointerException if appName is null
     */
    private static Path getXdgDir(String xdgEnvVar, String defaultRelativePath, String appName) {
        // 1. Check environment variable (e.g. $XDG_CONFIG_HOME)
        String xdgHome = System.getenv(xdgEnvVar);
        if (xdgHome != null && !xdgHome.trim().isEmpty()) {
            return Paths.get(xdgHome, appName);
        }

        // 2. Default path (e.g. ~/.config/)
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, defaultRelativePath, appName);
    }
}
