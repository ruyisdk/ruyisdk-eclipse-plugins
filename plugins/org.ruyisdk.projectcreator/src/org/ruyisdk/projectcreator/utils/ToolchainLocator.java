package org.ruyisdk.projectcreator.utils;

import java.io.File;
import java.nio.file.Paths;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.ruyisdk.core.exception.PluginException;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.packages.JsonParser;
import org.ruyisdk.projectcreator.Activator;

/**
 * Locates toolchain paths for boards.
 */
public class ToolchainLocator {
    private static final PluginLogger LOGGER = Activator.getLogger();
    private static final String PREF_LAST_TOOLCHAIN_PATH = "lastToolchainPath";

    /**
     * Finds toolchain path for the specified board.
     *
     * @param boardModel the board model
     * @return toolchain path or null
     */
    public static String findToolchainPathForBoard(String boardModel) {
        LOGGER.logInfo("Entering ToolchainLocator.findToolchainPathForBoard...");

        if (boardModel == null || boardModel.isEmpty()) {
            LOGGER.logError("boardModel is null or empty");
            return null;
        }
        LOGGER.logInfo("Received boardModel: " + boardModel);

        String toolchainName = null;
        try {
            // 1. jsonParser.findInstalledToolchainForBoard method
            LOGGER.logInfo("Preparing to call JsonParser.findInstalledToolchainForBoard...");
            toolchainName = JsonParser.findInstalledToolchainForBoard(boardModel);
            LOGGER.logInfo(
                    "Successfully called JsonParser. Returned toolchainName: " + toolchainName);
        } catch (PluginException e) {
            LOGGER.logError("Failed to find installed toolchain for board: " + boardModel, e);
            return null;
        }

        if (toolchainName == null || toolchainName.isEmpty()) {
            LOGGER.logError("toolchainName is null or empty. No toolchain found for this board");
            return null;
        }

        LOGGER.logInfo("Constructing toolchain path with name: " + toolchainName);
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            LOGGER.logError("user.home is null. Cannot construct path");
            return null;
        }
        File toolchainDir =
                Paths.get(userHome, ".local", "share", "ruyi", "binaries", "x86_64", toolchainName)
                        .toFile();
        LOGGER.logInfo("Constructed path: " + toolchainDir.getAbsolutePath());

        if (isValidToolchainPath(toolchainDir.getAbsolutePath())) {
            LOGGER.logInfo("Path is valid. Returning path");
            return toolchainDir.getAbsolutePath();
        }
        LOGGER.logError("Constructed path is not a valid toolchain path");
        return null;
    }

    /**
     * Gets the last used toolchain path.
     *
     * @return the last used toolchain path or null
     */
    public static String getLastUsedPath() {
        return getPreference(PREF_LAST_TOOLCHAIN_PATH);
    }

    /**
     * Saves last used toolchain path.
     *
     * @param path toolchain path
     */
    public static void saveLastUsedToolchainPath(String path) {
        setPreference(PREF_LAST_TOOLCHAIN_PATH, path);
    }

    private static boolean isValidToolchainPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        File binDir = new File(path, "bin");
        if (!binDir.exists() || !binDir.isDirectory()) {
            return false;
        }

        File[] files = binDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith("-gcc") && file.isFile() && file.canExecute()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getPreference(String key) {
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        return prefs.get(key, null);
    }

    private static void setPreference(String key, String value) {
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        try {
            prefs.put(key, value);
            prefs.flush();
        } catch (BackingStoreException e) {
            LOGGER.logError("Failed to save preference: " + key, e);
        }
    }
}
