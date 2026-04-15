package org.ruyisdk.ruyi.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.ruyisdk.core.basedir.XdgDirs;
import org.ruyisdk.core.config.Constants;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Properties manager for Ruyi configuration.
 */
public class RuyiProperties {
    private static final PluginLogger LOGGER = Activator.getLogger();
    private static final Path CONFIG_DIR = XdgDirs.getConfigDir(Constants.AppInfo.AppDir);
    private static final Path FILE_PATH = CONFIG_DIR.resolve(Constants.ConfigFile.RuyiProperties);
    private static final Properties props = loadConfig();

    // 初始化配置目录
    static {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(FILE_PATH)) {
                initDefaultConfig();
            }
            // Remove obsolete keys
            {
                var removed = false;
                for (final var key : new String[] {"ruyi.telemetry.status", "ruyi.mirror.custom",
                        "ruyi.mirror.iscas.checked", "ruyi.mirror.github.checked", "ruyi.mirror.custom.checked"}) {
                    if (props.remove(key) != null) {
                        removed = true;
                    }
                }
                if (removed) {
                    saveConfig();
                }
            }
        } catch (IOException e) {
            handleConfigError("Failed to init config directory", e);
        }
    }

    // 加载配置文件
    private static Properties loadConfig() {
        Properties loadedProps = new Properties();
        if (Files.exists(FILE_PATH)) {
            try (InputStream is = Files.newInputStream(FILE_PATH)) {
                loadedProps.load(is);
            } catch (IOException e) {
                handleConfigError("Failed to load config file", e);
            }
        }
        return loadedProps;
    }

    // 初始化默认配置
    private static void initDefaultConfig() throws IOException {
        Properties defaults = new Properties();
        defaults.setProperty("automatic.detection", "on");
        defaults.setProperty("ruyi.install.path", RuyiFileUtils.getDefaultInstallPath().toString());

        saveConfig(defaults);
        props.putAll(defaults); // 同时更新内存中的配置
    }

    // 修改saveConfig方法使其能接受Properties参数
    private static synchronized void saveConfig(Properties propsToSave) throws IOException {
        try (OutputStream os = Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
            propsToSave.store(os, "Ruyi Configuration");
        }
    }

    private static synchronized void saveConfig() throws IOException {
        saveConfig(props); // 重用上面的方法
    }

    // === 公开API ===//

    // 自动检测配置
    /**
     * Checks if automatic detection is enabled.
     *
     * @return true if enabled
     */
    public static boolean isAutomaticDetectionEnabled() {
        return "on".equalsIgnoreCase(props.getProperty("automatic.detection"));
    }

    /**
     * Sets automatic detection.
     *
     * @param enabled true to enable
     * @throws IOException if save fails
     */
    public static void setAutomaticDetection(boolean enabled) throws IOException {
        props.setProperty("automatic.detection", enabled ? "on" : "off");
        saveConfig();
    }

    // 安装路径配置
    /**
     * Gets install path.
     *
     * @return install path
     */
    public static String getInstallPath() {
        return props.getProperty("ruyi.install.path");
    }

    /**
     * Sets install path.
     *
     * @param path install path
     * @throws IOException if save fails
     */
    public static void setInstallPath(String path) throws IOException {
        props.setProperty("ruyi.install.path", path != null ? path : "");
        saveConfig();
    }

    // 错误处理
    private static void handleConfigError(String message, Exception e) {
        LOGGER.logError(message + ": " + e.getMessage());
        // if (Constants.DEBUG_MODE) {
        // e.printStackTrace();
        // }
    }
}
