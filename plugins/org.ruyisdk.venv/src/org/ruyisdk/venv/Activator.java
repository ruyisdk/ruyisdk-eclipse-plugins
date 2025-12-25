package org.ruyisdk.venv;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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

    private static final RuyiLogger LOGGER =
                    new RuyiLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    public VenvService getService() {
        return service;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.logInfo("Venv plugin starting");

        super.start(context);
        plugin = this;
        service = new VenvService();

        LOGGER.logInfo("Venv plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.logInfo("Venv plugin stopping");

        service = null;
        plugin = null;
        super.stop(context);

        LOGGER.logInfo("Venv plugin stopped");
    }

    /**
     * Returns a logger that does not depend on {@link #getDefault()} being initialized.
     *
     * <p>
     * This is safe to call during early class loading (e.g., before {@link #start(BundleContext)}).
     */
    public static RuyiLogger getLogger() {
        return LOGGER;
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
