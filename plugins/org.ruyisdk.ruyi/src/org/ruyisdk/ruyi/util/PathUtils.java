package org.ruyisdk.ruyi.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class PathUtils {

    /**
     * 检查PATH是否已包含指定路径
     * 
     * @param path 要检查的路径（自动标准化处理）
     * @return 是否已配置
     */
    public static boolean isPathConfigured(String path) {
        String normalizedPath = normalizePath(path);
        String currentPath = System.getenv("PATH");

        if (currentPath == null || currentPath.isEmpty()) {
            return false;
        }

        return Arrays.stream(currentPath.split(":")).map(PathUtils::normalizePath)
                        .anyMatch(p -> p.equals(normalizedPath));
    }

    /**
     * 标准化路径格式（统一处理末尾斜杠和符号链接）
     */
    private static String normalizePath(String rawPath) {
        try {
            // 解析符号链接并转为绝对路径
            Path path = Paths.get(rawPath).toAbsolutePath().normalize();
            // 统一去除末尾斜杠
            return path.toString().replaceAll("/+$", "");
        } catch (Exception e) {
            // 如果路径无效，返回原始值（标准化后）
            return rawPath.replaceAll("/+$", "");
        }
    }

    /**
     * 获取用户shell配置文件路径
     */
    public static Path detectShellConfigFile() throws IOException {
        String userHome = System.getProperty("user.home");
        String[] candidates = {".bashrc", ".zshrc", ".bash_profile", ".profile"};

        for (String filename : candidates) {
            Path configFile = Paths.get(userHome, filename);
            if (Files.isRegularFile(configFile)) {
                return configFile;
            }
        }
        return Paths.get(userHome, ".bashrc"); // 默认fallback
    }
}
