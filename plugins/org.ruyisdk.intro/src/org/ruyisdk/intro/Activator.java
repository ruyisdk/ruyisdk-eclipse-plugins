package org.ruyisdk.intro; // Updated package name

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator for the RuyiSDK Intro plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.intro"; // Updated Plugin ID
    private static Activator plugin;

    /**
     * Constructs the activator.
     */
    public Activator() {}

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

    public static Activator getDefault() {
        return plugin;
    }
}
