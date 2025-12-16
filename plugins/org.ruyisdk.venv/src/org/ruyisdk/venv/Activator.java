package org.ruyisdk.venv;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.venv.model.VenvService;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.ruyisdk.venv";

    // The shared instance
    private static Activator plugin;
    private static VenvService service;

    public VenvService getService() {
        return service;
    }

    /**
     * The constructor.
     */
    public Activator() {}

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        service = new VenvService();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        service = null;
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
}
