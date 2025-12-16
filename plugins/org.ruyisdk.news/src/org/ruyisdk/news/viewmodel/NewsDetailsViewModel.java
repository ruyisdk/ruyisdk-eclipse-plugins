package org.ruyisdk.news.viewmodel;

import org.ruyisdk.news.model.DataFetchService;
import org.ruyisdk.news.model.NewsItem;

/**
 * View model for fetching and exposing selected news details.
 */
public class NewsDetailsViewModel {

    private DataFetchService service;

    private boolean isFetching = false;


    /**
     * Creates a new view model.
     *
     * @param service the service used to fetch news details
     */

    public NewsDetailsViewModel(DataFetchService service) {
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

        selected.setUnread(false);
        selected.setDetails("<fetching news details>");
        isFetching = true;

        service.fetchNewsDetailsAsync(selected.getId(), result -> {
            isFetching = false;
            selected.setDetails(result == null ? "" : result);
            selected.setDetailsFetched(true);
        });
    }
}
