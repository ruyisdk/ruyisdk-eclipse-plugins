package org.ruyisdk.intro; // Updated package name

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;

/**
 * Activator for the RuyiSDK Intro plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.intro"; // Updated Plugin ID
    private static final PluginLogger LOGGER =
            new PluginLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);
    private static Activator plugin;

    /**
     * Starts the plugin.
     *
     * @param context the bundle context
     * @throws Exception if start fails
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /**
     * Stops the plugin.
     *
     * @param context the bundle context
     * @throws Exception if stop fails
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /** Returns the shared instance. */
    public static Activator getDefault() {
        return plugin;
    }

    /** Returns an Eclipse builtin logger. */
    public static PluginLogger getLogger() {
        return LOGGER;
    }
}
