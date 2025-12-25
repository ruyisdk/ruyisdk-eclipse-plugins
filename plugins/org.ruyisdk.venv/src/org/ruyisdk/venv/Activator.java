package org.ruyisdk.venv;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.ruyi.util.RuyiLogger;
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
    private RuyiLogger logger;

    public VenvService getService() {
        return service;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        logger = getLogger();
        service = new VenvService();

        logger.logInfo("Venv plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.logInfo("Venv plugin stopping");

        service = null;
        logger = null;
        plugin = null;
        super.stop(context);
    }

    /** Returns the plugin logger. */
    public RuyiLogger getLogger() {
        if (logger == null) {
            logger = new RuyiLogger(getLog(), PLUGIN_ID);
        }
        return logger;
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
