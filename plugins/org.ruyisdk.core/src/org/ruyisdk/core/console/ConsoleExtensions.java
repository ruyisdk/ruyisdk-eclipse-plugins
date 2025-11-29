package org.ruyisdk.core.console;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * 控制台扩展点支持.
 */
public class ConsoleExtensions {
    private static final String EXTENSION_POINT_ID = "org.ruyisdk.core.consoleExtensions";

    /**
     * Loads and initializes all registered console extensions.
     */
    public static void loadExtensions() {
        IConfigurationElement[] configs =
                        Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);

        for (IConfigurationElement config : configs) {
            try {
                Object ext = config.createExecutableExtension("class");
                if (ext instanceof ConsoleExtension) {
                    ((ConsoleExtension) ext).init(RuyiSdkConsole.getInstance());
                }
            } catch (CoreException e) {
                RuyiSdkConsole.getInstance().logError("Failed to load console extension: " + e.getMessage());
            }
        }
    }

    /**
     * Interface for console extension implementations.
     */
    public interface ConsoleExtension {
        /**
         * Initializes the extension with the console instance.
         *
         * @param console the console instance
         */
        void init(RuyiSdkConsole console);
    }
}
