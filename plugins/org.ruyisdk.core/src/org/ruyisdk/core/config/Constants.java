package org.ruyisdk.core.config;

/**
 * Constants 程序设计设定，用户不需要修改，由开发者在产品迭代优化中按需修改
 */
public final class Constants {

    public static final class AppInfo {
        public static final String AppDir = "ruyisdkide"; // XDG-Dir
    }

    public static final class ConfigFile {
        public static final String DeviceProperties = "devices.properties";
        public static final String RuyiProperties = "ruyi.properties";
    }

    // 网络端点（基础设施URL）
    public static final class NetAddress {
        // 官网
        public static final String WEBSIT = "https://ruyisdk.org";

        // Base域名
        private static final String MIRROR_BASE = "https://mirror.iscas.ac.cn";
        private static final String GITHUB_BASE = "https://github.com/ruyisdk";

        // 下载RUYI
        public static final String MIRROR_RUYI_RELEASES = MIRROR_BASE + "/ruyisdk/ruyi/releases";
        public static final String GITHUB_RUYI_RELEASES = GITHUB_BASE + "/ruyi/releases";

        // 下载IDE
        public static final String MIRROR_IDE_RELEASES = MIRROR_BASE + "/ruyisdk/ide";
        public static final String GITHUB_IDE_RELEASES = GITHUB_BASE + "/ruyisdk-eclipse-packages/releases";
        public static final String GITHUB_IDEPLUGINS_RELEASES = GITHUB_BASE + "/ruyisdk-eclipse-plugins/releases";

        // 存储库 packages-index
        public static final String MAIN_REPO_URL = GITHUB_BASE + "/packages-index.git"; // "https://github.com/ruyisdk/packages-index.git"
        public static final String BACKUP_REPO_URL = MIRROR_BASE + "/git/ruyisdk/packages-index.git"; // https://mirror.iscas.ac.cn/git/ruyisdk/packages-index.git
    }

    // 安装配置
    // 安装配置（设计期确定）
    public static final class Ruyi {
        public static String INSTALL_PATH = "~/.local/bin"; // ruyi install path;"/usr/local/bin/"
        public static final String BACKUP_PREFIX = "ruyi.backup.";
    }


}
