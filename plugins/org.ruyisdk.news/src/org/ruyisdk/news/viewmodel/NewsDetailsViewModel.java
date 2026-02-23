package org.ruyisdk.news.viewmodel;

import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.news.Activator;
import org.ruyisdk.news.model.NewsFetchService;
import org.ruyisdk.news.model.NewsItem;
import org.ruyisdk.news.util.MarkdownRenderer;

/**
 * View model for fetching and exposing selected news details.
 */
public class NewsDetailsViewModel {

    private static final PluginLogger LOGGER = Activator.getLogger();

    private NewsFetchService service;

    private boolean isFetching = false;


    /**
     * Creates a new view model.
     *
     * @param service the service used to fetch news details
     */

    public NewsDetailsViewModel(NewsFetchService service) {
        this.service = service;
    }


    /**
     * Fetches details for the selected item if not already fetched.
     *
     * @param selected the selected item
     */

    public void onAcquireNewsDetails(NewsItem selected) {
        if (selected.getDetailsFetched()) {
            return;
        }
        if (isFetching) {
            return;
        }

        LOGGER.logInfo("News details requested: id=" + selected.getId());

        selected.setUnread(false);
        selected.setDetailsHtml(MarkdownRenderer.renderToHtml("*fetching news details...*"));
        isFetching = true;

        service.fetchNewsDetailsAsync(selected.getId(), result -> {
            isFetching = false;
            final var markdown = result == null ? "" : result;
            selected.setDetails(markdown);
            selected.setDetailsHtml(MarkdownRenderer.renderToHtml(markdown));
            selected.setDetailsFetched(true);
        }, result -> {
            isFetching = false;
            final var errorMarkdown = "*failed to fetch news details: " + result + "*";
            selected.setDetails(errorMarkdown);
            selected.setDetailsHtml(MarkdownRenderer.renderToHtml(errorMarkdown));
            selected.setDetailsFetched(false);
        });
    }
}
