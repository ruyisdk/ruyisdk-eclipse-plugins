package org.ruyisdk.core.basedir;

import java.nio.file.Path;

import org.junit.Ignore;
import org.junit.Test;
import org.ruyisdk.core.config.Constants;

/**
 * Demonstrates usage of XDG base directory specification to get standard directories.
 *
 * <p>
 * This class shows how to retrieve and use the standard XDG directories (config, cache, data, and
 * state) for application storage following the XDG Base Directory Specification.
 */
public class XdgDirsTest {

    @Ignore("Demo test: depends on local environment")
    @Test
    public void testXdgDirs() {
        // Get application name from constants
        String appName = Constants.AppInfo.AppDir;

        // Retrieve standard XDG directories
        Path configDir = XdgDirs.getConfigDir(appName);
        Path cacheDir = XdgDirs.getCacheDir(appName);
        Path dataDir = XdgDirs.getDataDir(appName);
        Path stateDir = XdgDirs.getStateDir(appName);

        // Print directory paths
        System.out.println("Config Dir: " + configDir);
        System.out.println("Cache Dir: " + cacheDir);
        System.out.println("Data Dir: " + dataDir);
        System.out.println("State Dir: " + stateDir);

        // Ensure directories exist (create if they don't)
        configDir.toFile().mkdirs();
        // cacheDir.toFile().mkdirs();
        // dataDir.toFile().mkdirs();
        // stateDir.toFile().mkdir();
    }
}
