package org.ruyisdk.ruyi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
     * Ensures directory exists.
     *
     * @param path directory path
     * @return true if created
     * @throws IOException if creation fails
     */
    public static boolean ensureDirectoryExists(String path) throws IOException {
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            return true;
        }
        return Files.isDirectory(dirPath);
    }

    /**
     * Checks if file is executable.
     *
     * @param filePath file path
     * @return true if executable
     */
    public static boolean isExecutable(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isExecutable(path);
    }

    /**
     * Reads file content.
     *
     * @param filePath file path
     * @return file content
     * @throws IOException if read fails
     */
    public static String readFileContent(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Writes content to file.
     *
     * @param filePath file path
     * @param content content to write
     * @throws IOException if write fails
     */
    public static void writeFileContent(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Deletes path recursively.
     *
     * @param path path to delete
     * @return true if deleted
     * @throws IOException if delete fails
     */
    public static boolean deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(p -> {
                try {
                    deleteRecursively(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return Files.deleteIfExists(path);
    }
}
