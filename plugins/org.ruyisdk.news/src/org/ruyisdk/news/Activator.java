package org.ruyisdk.news;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.news.model.NewsFetchService;
import org.ruyisdk.news.model.NewsManager;

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

    public NewsManager getNewsManager() {
        return newsManager;
    }

    public NewsFetchService getService() {
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

        newsManager = new NewsManager();
        service = new NewsFetchService();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        newsManager = null;
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
