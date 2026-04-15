package org.ruyisdk.packages;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;

/**
 * Activator for the packages plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.packages";
    private static Activator plugin;

    private static final PluginLogger LOGGER =
                    new PluginLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /** Returns an Eclipse builtin logger. */
    public static PluginLogger getLogger() {
        return LOGGER;
    }

    /** Returns the shared instance. */
    public static Activator getDefault() {
        return plugin;
    }
}
