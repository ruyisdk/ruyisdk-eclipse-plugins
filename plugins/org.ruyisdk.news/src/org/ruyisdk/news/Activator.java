package org.ruyisdk.news;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.news.model.NewsFetchService;
import org.ruyisdk.news.model.NewsManager;
import org.ruyisdk.ruyi.util.RuyiLogger;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.ruyisdk.news";

    // The shared instance
    private static Activator plugin;
    private static NewsManager newsManager;
    private static NewsFetchService service;

    private static final RuyiLogger LOGGER =
                    new RuyiLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    public NewsManager getNewsManager() {
        return newsManager;
    }

    public NewsFetchService getService() {
        return service;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.logInfo("News plugin starting");

        super.start(context);
        plugin = this;
        newsManager = new NewsManager();
        service = new NewsFetchService();

        LOGGER.logInfo("News plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOGGER.logInfo("News plugin stopping");

        newsManager = null;
        service = null;
        plugin = null;
        super.stop(context);

        LOGGER.logInfo("News plugin stopped");
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
