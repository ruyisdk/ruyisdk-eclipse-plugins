package org.ruyisdk.projectcreator.utils;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.ruyisdk.packages.JsonParser;
import org.ruyisdk.projectcreator.Activator;

public class ToolchainLocator {

	private static final String PREF_LAST_TOOLCHAIN_PATH = "lastToolchainPath";

	public static String findToolchainPathForBoard(String boardModel) {
		System.out.println("[DEBUG] Entering ToolchainLocator.findToolchainPathForBoard...");

		if (boardModel == null || boardModel.isEmpty()) {
			System.out.println("[DEBUG] boardModel is null or empty. Exiting.");
			return null;
		}
		System.out.println("[DEBUG] Received boardModel: " + boardModel);

		String toolchainName = null;
		try {
			// 1. jsonParser.findInstalledToolchainForBoard method
			System.out.println("[DEBUG] Preparing to call JsonParser.findInstalledToolchainForBoard...");
			toolchainName = JsonParser.findInstalledToolchainForBoard(boardModel);
			System.out.println("[DEBUG] Successfully called JsonParser. Returned toolchainName: " + toolchainName);

		} catch (NoClassDefFoundError e) {
			System.err.println("[DEBUG] CRITICAL: NoClassDefFoundError caught!");
			System.err.println("[DEBUG] Failed to find or load class: " + e.getMessage());
			e.printStackTrace();
			return null;
		} catch (Throwable t) {
			System.err.println("[DEBUG] CRITICAL: An unexpected error or exception occurred!");
			t.printStackTrace();
			return null;
		}

		if (toolchainName == null || toolchainName.isEmpty()) {
			System.out.println("[DEBUG] toolchainName is null or empty. No toolchain found for this board.");
			return null;
		}

		System.out.println("[DEBUG] Constructing toolchain path with name: " + toolchainName);
		String userHome = System.getProperty("user.home");
		if (userHome == null) {
			System.err.println("[DEBUG] user.home is null. Cannot construct path.");
			return null;
		}
		File toolchainDir = Paths.get(userHome, ".local", "share", "ruyi", "binaries", "x86_64", toolchainName)
				.toFile();
		System.out.println("[DEBUG] Constructed path: " + toolchainDir.getAbsolutePath());

		if (isValidToolchainPath(toolchainDir.getAbsolutePath())) {
			System.out.println("[DEBUG] Path is valid. Returning path.");
			return toolchainDir.getAbsolutePath();
		} else {
			System.err.println("[DEBUG] Constructed path is not a valid toolchain path.");
		}

		return null;
	}

	public static String getLastUsedPath() {
		return getPreference(PREF_LAST_TOOLCHAIN_PATH);
	}

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
			e.printStackTrace();
		}
	}
}
