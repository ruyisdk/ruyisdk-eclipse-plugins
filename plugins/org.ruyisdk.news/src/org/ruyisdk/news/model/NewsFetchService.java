package org.ruyisdk.news.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.news.Activator;
import org.ruyisdk.ruyi.services.RuyiCli;
import org.ruyisdk.ruyi.util.RuyiLogger;

/** Service for fetching news data via the Ruyi CLI. */
public class NewsFetchService {
    private static final RuyiLogger LOGGER = Activator.getLogger();

    /** Fetches news details asynchronously. */
    public void fetchNewsDetailsAsync(String id, Consumer<String> callback) {
        fetchNewsDetailsAsync(id, callback, null);
    }

    /** Fetches news details asynchronously with an optional error callback. */
    public void fetchNewsDetailsAsync(String id, Consumer<String> callback, Consumer<String> errorCallback) {
        Job fetchJob = new Job("Fetching News Details") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                LOGGER.logInfo("Fetching news details: id=" + id);
                String result = "";
                try {
                    RuyiCli.NewsReadResult read = RuyiCli.readNewsItem(id);
                    if (read != null) {
                        result = read.getContent() == null ? "" : read.getContent();
                    }
                    LOGGER.logInfo("Fetched news details: id=" + id + ", length=" + result.length());
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "Failed to read news details" : e.getMessage();
                    LOGGER.logError("Failed to fetch news details: id=" + id, e);
                    if (errorCallback != null) {
                        errorCallback.accept(msg);
                    }
                    result = "";
                }
                callback.accept(result);
                return Status.OK_STATUS;
            }
        };
        fetchJob.schedule();
    }

    /** Fetches the news list asynchronously. */
    public void fetchNewsListAsync(Consumer<List<NewsItem>> callback) {
        fetchNewsListAsync(callback, null);
    }

    /** Fetches the news list asynchronously with an optional error callback. */
    public void fetchNewsListAsync(Consumer<List<NewsItem>> callback, Consumer<String> errorCallback) {
        Job fetchJob = new Job("Fetching News List") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                LOGGER.logInfo("Fetching news list");
                var newsList = new ArrayList<NewsItem>();
                try {
                    int unreadCount = 0;
                    for (var item : RuyiCli.listNewsItems(false)) {
                        if (item == null) {
                            continue;
                        }

                        final Boolean isRead = item.isRead();
                        boolean unread = isRead == null || !isRead.booleanValue();
                        if (unread) {
                            unreadCount++;
                        }

                        final Integer ordObj = item.getOrd();
                        final int ord = ordObj == null ? -1 : ordObj.intValue();
                        final String title = item.getTitle() == null ? "" : item.getTitle();
                        final String id = item.getId() == null ? "" : item.getId();
                        newsList.add(new NewsItem(ord, title, id, unread));
                    }
                    LOGGER.logInfo("Fetched news list: count=" + newsList.size() + ", unread=" + unreadCount);
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "Failed to list news" : e.getMessage();
                    LOGGER.logError("Failed to fetch news list", e);
                    if (errorCallback != null) {
                        errorCallback.accept(msg);
                    }
                }

                callback.accept(newsList);

                return Status.OK_STATUS;
            }
        };
        fetchJob.schedule();
    }
}
