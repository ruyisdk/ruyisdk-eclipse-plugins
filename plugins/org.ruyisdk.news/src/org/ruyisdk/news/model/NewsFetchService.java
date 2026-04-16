package org.ruyisdk.news.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.news.Activator;
import org.ruyisdk.ruyi.services.RuyiCli;

/** Service for fetching news data via the Ruyi CLI. */
public class NewsFetchService {
    private static final PluginLogger LOGGER = Activator.getLogger();

    /** Fetches news details asynchronously with an optional error callback. */
    public void fetchNewsDetailsAsync(String id, Consumer<String> callback, Consumer<String> errorCallback) {
        final var fetchJob = Job.create("Fetching News Details", monitor -> {
            LOGGER.logInfo("Fetching news details: id=" + id);
            try {
                final var result = RuyiCli.readNewsItem(id);
                if (result == null) {
                    throw new Exception("timed out");
                }
                final var content = result.getContent() == null ? "" : result.getContent();
                LOGGER.logInfo("Fetched news details: id=" + id + ", length=" + content.length());
                callback.accept(content);
                return Status.OK_STATUS;
            } catch (Exception e) {
                LOGGER.logError("Failed to fetch news details: id=" + id, e);
                final var msg = e.getMessage() == null ? "Failed to read news details" : e.getMessage();
                if (errorCallback != null) {
                    errorCallback.accept(msg);
                }
                return Status.CANCEL_STATUS; // avoid Eclipse error dialog
            }
        });
        fetchJob.schedule();
    }

    /** Fetches the news list asynchronously. */
    public void fetchNewsListAsync(Consumer<List<NewsItem>> callback) {
        final var fetchJob = Job.create("Fetching News List", monitor -> {
            LOGGER.logInfo("Fetching news list");
            try {
                final var newsList = new ArrayList<NewsItem>();
                int unreadCount = 0;
                for (final var item : RuyiCli.listNewsItems(false)) {
                    if (item == null) {
                        continue;
                    }

                    final var isRead = item.isRead();
                    final var unread = isRead == null || !isRead.booleanValue();
                    if (unread) {
                        unreadCount++;
                    }

                    final var ordObj = item.getOrd();
                    final var ord = ordObj == null ? -1 : ordObj.intValue();
                    final var title = item.getTitle() == null ? "" : item.getTitle();
                    final var id = item.getId() == null ? "" : item.getId();
                    newsList.add(new NewsItem(ord, title, id, unread));
                }
                LOGGER.logInfo("Fetched news list: count=" + newsList.size() + ", unread=" + unreadCount);
                callback.accept(newsList);
                return Status.OK_STATUS;
            } catch (Exception e) {
                callback.accept(null);
                return Status.error("Failed to fetch news list", e);
            }
        });
        fetchJob.schedule();
    }
}
