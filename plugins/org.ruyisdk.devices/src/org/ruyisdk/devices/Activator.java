package org.ruyisdk.devices;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "org.ruyisdk.devices";
    private static final PluginLogger LOGGER =
                    new PluginLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    // The shared instance
    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        LOGGER.logInfo("Devices Plugin " + getBundle().getVersion() + " Activated !");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    public static PluginLogger getLogger() {
        return LOGGER;
    }
}
