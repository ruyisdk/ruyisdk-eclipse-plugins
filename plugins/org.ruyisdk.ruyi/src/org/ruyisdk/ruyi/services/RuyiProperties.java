package org.ruyisdk.ruyi.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Properties;
import org.ruyisdk.core.basedir.XdgDirs;
import org.ruyisdk.core.config.Constants;
import org.ruyisdk.core.config.Constants.AppInfo;
import org.ruyisdk.core.config.Constants.ConfigFile;
import org.ruyisdk.core.config.Constants.Ruyi;
import org.ruyisdk.core.console.RuyiSdkConsole;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Properties manager for Ruyi configuration.
 */
public class RuyiProperties {
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
        defaults.setProperty("ruyi.telemetry.status", "on");
        defaults.setProperty("ruyi.install.path", RuyiFileUtils.getDefaultInstallPath().toString());
        defaults.setProperty("ruyi.mirror.custom", "");

        // 为package-index设置默认的选中状态
        defaults.setProperty("ruyi.mirror.iscas.checked", "1");
        defaults.setProperty("ruyi.mirror.github.checked", "1");
        defaults.setProperty("ruyi.mirror.custom.checked", "0");

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

    // 镜像仓库配置
    /**
     * Gets custom mirror URL.
     *
     * @return mirror URL
     */
    public static String getCustomMirror() {
        return props.getProperty("ruyi.mirror.custom");
    }

    /**
     * Sets custom mirror URL.
     *
     * @param url mirror URL
     * @throws IOException if save fails
     */
    public static void setCustomMirror(String url) throws IOException {
        props.setProperty("ruyi.mirror.custom", url != null ? url : "");
        saveConfig();
    }

    /**
     * Checks if ISCAS mirror is checked.
     *
     * @return true if checked
     */
    public static boolean isIscasMirrorChecked() {
        return "1".equals(props.getProperty("ruyi.mirror.iscas.checked"));
    }

    /**
     * Sets ISCAS mirror checked state.
     *
     * @param checked checked state
     * @throws IOException if save fails
     */
    public static void setIscasMirrorChecked(boolean checked) throws IOException {
        props.setProperty("ruyi.mirror.iscas.checked", checked ? "1" : "0");
        saveConfig();
    }

    /**
     * Checks if GitHub mirror is checked.
     *
     * @return true if checked
     */
    public static boolean isGithubMirrorChecked() {
        return "1".equals(props.getProperty("ruyi.mirror.github.checked"));
    }

    /**
     * Sets GitHub mirror checked state.
     *
     * @param checked checked state
     * @throws IOException if save fails
     */
    public static void setGithubMirrorChecked(boolean checked) throws IOException {
        props.setProperty("ruyi.mirror.github.checked", checked ? "1" : "0");
        saveConfig();
    }

    /**
     * Checks if custom mirror is checked.
     *
     * @return true if checked
     */
    public static boolean isCustomMirrorChecked() {
        return "1".equals(props.getProperty("ruyi.mirror.custom.checked"));
    }

    /**
     * Sets custom mirror checked state.
     *
     * @param checked checked state
     * @throws IOException if save fails
     */
    public static void setCustomMirrorChecked(boolean checked) throws IOException {
        props.setProperty("ruyi.mirror.custom.checked", checked ? "1" : "0");
        saveConfig();
    }

    // 遥测配置
    /**
     * Gets telemetry status.
     *
     * @return telemetry status
     */
    public static TelemetryStatus getTelemetryStatus() {
        try {
            return TelemetryStatus.valueOf(props.getProperty("ruyi.telemetry.status").toUpperCase());
        } catch (IllegalArgumentException e) {
            return TelemetryStatus.ON;
        }
    }

    /**
     * Sets telemetry status.
     *
     * @param status telemetry status
     * @throws IOException if save fails
     */
    public static void setTelemetryStatus(TelemetryStatus status) throws IOException {
        props.setProperty("ruyi.telemetry.status", status.name().toLowerCase(Locale.ROOT));
        saveConfig();
    }

    /**
     * Sets telemetry status from boolean.
     *
     * @param status true for on, false for off
     * @throws IOException if save fails
     */
    public static void setTelemetryStatus(boolean status) throws IOException {
        props.setProperty("ruyi.telemetry.status", status ? "on" : "off");
        saveConfig();
    }



    // 遥测状态枚举
    /**
     * Telemetry status enumeration.
     */
    public enum TelemetryStatus {
        ON, // 完全启用
        LOCAL, // 仅本地分析
        OFF // 完全禁用
    }

    // 错误处理
    private static void handleConfigError(String message, Exception e) {
        RuyiSdkConsole.getInstance().logError(message + ": " + e.getMessage());
        // if (Constants.DEBUG_MODE) {
        // e.printStackTrace();
        // }
    }
}
