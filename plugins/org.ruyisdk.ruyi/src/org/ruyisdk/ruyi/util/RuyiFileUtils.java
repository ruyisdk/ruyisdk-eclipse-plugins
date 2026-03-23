package org.ruyisdk.ruyi.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.ruyisdk.core.config.Constants;
import org.ruyisdk.ruyi.services.RuyiProperties;

/**
 * File utility methods for Ruyi.
 */
public class RuyiFileUtils {


    /**
     * Gets the default install path.
     *
     * @return default install path
     */
    public static Path getDefaultInstallPath() {
        // 获取配置路径并标准化处理
        String ruyiDefaultInstallDir = Constants.Ruyi.INSTALL_PATH.trim();

        // 处理 ~ 和 ~user 形式的路径
        if (ruyiDefaultInstallDir.startsWith("~")) {
            ruyiDefaultInstallDir = ruyiDefaultInstallDir.replaceFirst("^~", System.getProperty("user.home"));
        }

        // 使用NIO API解析路径（自动处理跨平台分隔符）
        Path path = Paths.get(ruyiDefaultInstallDir);

        // 转换为绝对路径
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath().normalize();
        }

        return path;
    }

    /**
     * Gets the install path from config or default.
     *
     * @return install path
     */
    public static String getInstallPath() {
        String path = RuyiProperties.getInstallPath();
        // System.out.println("Ruyi install path set by the user is :" + path);
        if (path == null || path.trim().isEmpty()) {
            path = getDefaultInstallPath().toString();
        }
        // System.out.println("Ruyi install path using default set : " + path);
        return path;
    }

    /**
     * Finds the installation path that contains an executable {@code ruyi} binary.
     *
     * <p>
     * Lookup order is:
     * <ol>
     * <li>configured install path from properties</li>
     * <li>default install path</li>
     * </ol>
     *
     * @return resolved install path, or empty string if not found
     */
    public static String findInstallPathWithRuyi() {
        final var configuredPath = normalizePath(RuyiProperties.getInstallPath());
        if (!configuredPath.isEmpty() && hasExecutableRuyi(configuredPath)) {
            return configuredPath;
        }

        final var defaultPath = getDefaultInstallPath().toString();
        if (hasExecutableRuyi(defaultPath)) {
            return defaultPath;
        }
        return "";
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith("~")) {
            normalized = normalized.replaceFirst("^~", System.getProperty("user.home"));
        }
        final var nioPath = Paths.get(normalized);
        return nioPath.toAbsolutePath().normalize().toString();
    }

    private static boolean hasExecutableRuyi(String installPath) {
        if (installPath == null || installPath.isBlank()) {
            return false;
        }
        final var path = Paths.get(Paths.get(installPath, "ruyi").toString());
        return Files.exists(path) && Files.isExecutable(path);
    }
}
