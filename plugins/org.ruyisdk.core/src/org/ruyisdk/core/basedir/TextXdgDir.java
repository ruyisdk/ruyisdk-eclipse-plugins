package org.ruyisdk.core.basedir;

import java.nio.file.Path;

import org.ruyisdk.core.config.Constants;

public class TextXdgDir {

    public static void main(String[] args) {
        String appName = Constants.AppInfo.AppDir;

        // 获取 XDG 目录
        Path configDir = XdgDirs.getConfigDir(appName);
        Path cacheDir = XdgDirs.getCacheDir(appName);
        Path dataDir = XdgDirs.getDataDir(appName);
        Path stateDir = XdgDirs.getStateDir(appName);

        System.out.println("Config Dir: " + configDir);
        System.out.println("Cache Dir: " + cacheDir);
        System.out.println("Data Dir: " + dataDir);
        System.out.println("State Dir: " + stateDir);

        // 确保目录存在（如果不存在则创建）
        configDir.toFile().mkdirs();
        // cacheDir.toFile().mkdirs();
        // dataDir.toFile().mkdirs();
        // stateDir.toFile().mkdir();
    }

}
