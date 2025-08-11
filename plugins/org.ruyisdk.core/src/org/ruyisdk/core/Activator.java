package org.ruyisdk.core;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.core.console.ConsoleExtensions;
import org.ruyisdk.core.console.ConsoleManager;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "org.ruyisdk.core"; // The plug-in ID
    private static Activator plugin; // The shared instance


    /**
     * Constructs a new Activator.
     *
     */
    public Activator() {
        // Parent class AbstractUIPlugin handles initialization automatically
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // Initialize console extensions
        ConsoleExtensions.loadExtensions();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ConsoleManager.dispose();

        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return the shared plugin instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given path.
     *
     * @param path the image path (relative to the plugin installation)
     * @return the image descriptor, or null if the image could not be found
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
