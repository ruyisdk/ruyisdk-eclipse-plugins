package org.ruyisdk.news.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.news.Activator;
import org.ruyisdk.ruyi.services.RuyiCli;

/** Service for fetching news data via the Ruyi CLI. */
public class NewsFetchService {
    private static final PluginLogger LOGGER = Activator.getLogger();

    /** Fetches news details asynchronously. */
    public CompletableFuture<String> fetchNewsDetailsAsync(String id) {
        final var future = new CompletableFuture<String>();
        final var job = Job.create("Fetching News Details", monitor -> {
            LOGGER.logInfo("Fetching news details, id=" + id);
            try {
                final var result = RuyiCli.readNewsItem(id);
                if (result == null || result.getContent() == null) {
                    // TODO: do not use runtimeException.
                    throw new RuntimeException("News item not found, id=" + id);
                }
                LOGGER.logInfo("Fetched news details, id=" + id);
                future.complete(result.getContent());
                return Status.OK_STATUS;
            } catch (Exception e) {
                future.completeExceptionally(e);
                return Status.error("Failed to read news details, id=" + id, e);
            }
        });
        // no error dialog
        job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
        job.schedule();
        return future;
    }

    /** Fetches the news list asynchronously. */
    public CompletableFuture<List<NewsItem>> fetchNewsListAsync() {
        final var future = new CompletableFuture<List<NewsItem>>();
        final var job = Job.create("Fetching News List", monitor -> {
            LOGGER.logInfo("Fetching news list");
            try {
                final var fetchedItems = RuyiCli.listNewsItems(false);
                LOGGER.logInfo(String.format("Fetched news list: count=%d", fetchedItems.size()));

                final var newsList = new ArrayList<NewsItem>();
                for (final var item : fetchedItems) {
                    if (item == null) {
                        continue;
                    }
                    final var ordObj = item.getOrd();
                    final var ord = ordObj == null ? -1 : ordObj.intValue();
                    final var title = item.getTitle() == null ? "" : item.getTitle();
                    final var id = item.getId() == null ? "" : item.getId();
                    final var isRead = item.isRead();
                    final var unread = isRead == null || !isRead.booleanValue();
                    newsList.add(new NewsItem(ord, title, id, unread));
                }
                newsList.sort(Comparator.comparingInt(NewsItem::getOrd).reversed());
                future.complete(newsList);
                return Status.OK_STATUS;
            } catch (Exception e) {
                future.completeExceptionally(e);
                return Status.error("Failed to fetch news list", e);
            }
        });
        // no error dialog
        job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
        job.schedule();
        return future;
    }
}
