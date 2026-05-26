package org.ruyisdk.ruyi;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.core.workspace.WorkspaceProjectsMonitor;

/**
 * Activator for the Ruyi plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.ruyi";
    private static Activator plugin; // 共享实例

    private static final PluginLogger LOGGER =
            new PluginLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    /**
     * Starts the plugin.
     *
     * @param context bundle context
     * @throws Exception if start fails
     */
    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.logInfo("Ruyi plugin starting");

        super.start(context);
        plugin = this;

        LOGGER.logInfo("Ruyi plugin started");
    }

    /**
     * Stops the plugin.
     *
     * @param context bundle context
     * @throws Exception if stop fails
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.logInfo("Ruyi plugin stopping");

        WorkspaceProjectsMonitor.getInstance().dispose();
        plugin = null;
        super.stop(context);

        LOGGER.logInfo("Ruyi plugin stopped");
    }

    /** Returns the shared instance. */
    public static Activator getDefault() {
        return plugin;
    }

    /** Returns an Eclipse builtin logger. */
    public static PluginLogger getLogger() {
        return LOGGER;
    }

    @Override
    public IPreferenceStore getPreferenceStore() {
        return super.getPreferenceStore();
    }
}
