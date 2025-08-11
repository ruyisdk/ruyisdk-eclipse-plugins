package org.ruyisdk.core.basedir;

import java.nio.file.Path;
import java.nio.file.Paths;

public class XdgDirs {

    /**
     * 获取 XDG 配置目录（$XDG_CONFIG_HOME/<app-name> 或 ~/.config/<app-name>）
     */
    public static Path getConfigDir(String appName) {
        return getXdgDir("XDG_CONFIG_HOME", ".config", appName);
    }

    /**
     * 获取 XDG 缓存目录（$XDG_CACHE_HOME/<app-name> 或 ~/.cache/<app-name>）
     */
    public static Path getCacheDir(String appName) {
        return getXdgDir("XDG_CACHE_HOME", ".cache", appName);
    }

    /**
     * 获取 XDG 数据目录（$XDG_DATA_HOME/<app-name> 或 ~/.local/share/<app-name>）
     */
    public static Path getDataDir(String appName) {
        return getXdgDir("XDG_DATA_HOME", ".local/share", appName);
    }
    
    /**
     * 获取 XDG 状态目录（$XDG_STATE_HOME/<app-name> 或 ~/.local/state/<app-name>）
     */
    public static Path getStateDir(String appName) {
        return getXdgDir("XDG_STATE_HOME", ".local/state", appName);
    }

    /**
     * 通用方法：获取 XDG 目录
     * @param xdgEnvVar 环境变量名（如 "XDG_CONFIG_HOME"）
     * @param defaultRelativePath 默认相对路径（如 ".config"）
     * @param appName 应用名称（如 "your-ide-name"）
     * @return 完整路径（如 /home/user/.config/your-ide-name）
     */
    private static Path getXdgDir(String xdgEnvVar, String defaultRelativePath, String appName) {
        // 1. 检查环境变量（如 $XDG_CONFIG_HOME）
        String xdgHome = System.getenv(xdgEnvVar);
        if (xdgHome != null && !xdgHome.trim().isEmpty()) {
            return Paths.get(xdgHome, appName);
        }

        // 2. 默认路径（如 ~/.config/）
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, defaultRelativePath, appName);
    }
}
