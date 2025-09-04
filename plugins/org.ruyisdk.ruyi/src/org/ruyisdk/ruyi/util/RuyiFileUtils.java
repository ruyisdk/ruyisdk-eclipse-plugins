package org.ruyisdk.ruyi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import org.ruyisdk.core.config.Constants;
import org.ruyisdk.ruyi.services.RuyiProperties;

public class RuyiFileUtils {


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

    // 如果配置文件中没有设置 ruyi.install.path，则返回程序默认设置 Constants.Ruyi.INSTALL_PATH
    public static String getInstallPath() {
        String path = RuyiProperties.getInstallPath();
        // System.out.println("Ruyi install path set by the user is :" + path);
        if (path == null || path.trim().isEmpty()) {
            path = getDefaultInstallPath().toString();
        }
        // System.out.println("Ruyi install path using default set : " + path);
        return path;
    }

    public static boolean ensureDirectoryExists(String path) throws IOException {
        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            return true;
        }
        return Files.isDirectory(dirPath);
    }

    public static boolean isExecutable(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isExecutable(path);
    }

    public static String readFileContent(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static void writeFileContent(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
    }

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
