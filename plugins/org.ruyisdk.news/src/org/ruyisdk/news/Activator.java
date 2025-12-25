package org.ruyisdk.news;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
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
    private RuyiLogger logger;

    public NewsManager getNewsManager() {
        return newsManager;
    }

    public NewsFetchService getService() {
        return service;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        logger = getLogger();
        newsManager = new NewsManager();
        service = new NewsFetchService();

        logger.logInfo("News plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.logInfo("News plugin stopping");

        newsManager = null;
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
