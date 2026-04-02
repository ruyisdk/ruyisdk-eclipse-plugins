package org.ruyisdk.promotion;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** Plugin lifecycle activator. */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.promotion";

    private static Activator plugin;

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

    /** Returns the shared instance. */
    public static Activator getDefault() {
        return plugin;
    }
}
