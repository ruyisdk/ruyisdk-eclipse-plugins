package org.ruyisdk.venv;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.venv.model.VenvConfigurationService;
import org.ruyisdk.venv.model.VenvDetectionService;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.ruyisdk.venv";

    // The shared instance
    private static Activator plugin;
    private static VenvDetectionService detectionService;
    private static VenvConfigurationService configService;

    private static final PluginLogger LOGGER =
                    new PluginLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    /** Returns the venv service. */
    public VenvDetectionService getDetectionService() {
        return detectionService;
    }

    /** Returns the venv configuration service. */
    public VenvConfigurationService getConfigService() {
        return configService;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.logInfo("Venv plugin starting");

        super.start(context);
        plugin = this;
        detectionService = new VenvDetectionService();
        configService = new VenvConfigurationService();

        LOGGER.logInfo("Venv plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.logInfo("Venv plugin stopping");

        configService = null;
        detectionService = null;
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
    public static PluginLogger getLogger() {
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
