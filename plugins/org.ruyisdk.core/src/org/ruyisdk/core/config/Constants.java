package org.ruyisdk.core.config;

/**
 * Centralized configuration constants for RuyiSDK.
 *
 * <p>These are design-time constants that should only be modified by developers during product
 * iteration. End users should not modify these values.
 */
public final class Constants {

    /**
     * Application information constants.
     */
    public static final class AppInfo {
        /**
         * XDG base directory name following XDG Base Directory Specification.
         * Example: "~/.config/ruyisdkide"
         */
        public static final String AppDir = "ruyisdkide";
    }

    /**
     * Configuration file names and paths.
     */
    public static final class ConfigFile {
        /** Device-specific properties file name. */
        public static final String DeviceProperties = "devices.properties";

        /** Main RuyiSDK properties file name. */
        public static final String RuyiProperties = "ruyi.properties";
    }

    /**
     * Network endpoints and infrastructure URLs.
     */
    public static final class NetAddress {
        /** Official website URL. */
        public static final String WEBSITE = "https://ruyisdk.org";

        // Base domains
        private static final String MIRROR_BASE = "https://mirror.iscas.ac.cn";
        private static final String GITHUB_BASE = "https://github.com/ruyisdk";

        /** Mirror server URL for RuyiSDK releases. */
        public static final String MIRROR_RUYI_RELEASES = MIRROR_BASE + "/ruyisdk/ruyi/releases";

        /** GitHub URL for RuyiSDK releases. */
        public static final String GITHUB_RUYI_RELEASES = GITHUB_BASE + "/ruyi/releases";

        /** Mirror server URL for IDE releases. */
        public static final String MIRROR_IDE_RELEASES = MIRROR_BASE + "/ruyisdk/ide";

        /** GitHub URL for IDE packages releases. */
        public static final String GITHUB_IDE_RELEASES = GITHUB_BASE + "/ruyisdk-eclipse-packages/releases";

        /** GitHub URL for IDE plugins releases. */
        public static final String GITHUB_IDEPLUGINS_RELEASES = GITHUB_BASE + "/ruyisdk-eclipse-plugins/releases";

        /** Main repository URL for packages index. */
        public static final String MAIN_REPO_URL = GITHUB_BASE + "/packages-index.git";

        /** Backup repository URL for packages index. */
        public static final String BACKUP_REPO_URL = MIRROR_BASE + "/git/ruyisdk/packages-index.git";
    }

    /**
     * Installation configuration constants.
     */
    public static final class Ruyi {
        /**
         * Default installation path for RuyiSDK.
         * Default value: "~/.local/bin" (user-local binaries)
         */
        public static String INSTALL_PATH = "~/.local/bin";

        /** Prefix for backup files created by RuyiSDK. */
        public static final String BACKUP_PREFIX = "ruyi.backup.";
    }
}