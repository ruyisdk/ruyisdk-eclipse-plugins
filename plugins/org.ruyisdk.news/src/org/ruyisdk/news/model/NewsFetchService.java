package org.ruyisdk.news.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.ruyi.services.RuyiCli;

/** Service for fetching news data via the Ruyi CLI. */
public class NewsFetchService {
    /** Fetches news details asynchronously. */
    public void fetchNewsDetailsAsync(String id, Consumer<String> callback) {
        fetchNewsDetailsAsync(id, callback, null);
    }

    /** Fetches news details asynchronously with an optional error callback. */
    public void fetchNewsDetailsAsync(String id, Consumer<String> callback, Consumer<String> errorCallback) {
        Job fetchJob = new Job("Fetching News Details") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String result = "";
                try {
                    RuyiCli.NewsReadResult read = RuyiCli.readNewsItem(id);
                    if (read != null) {
                        result = read.getContent() == null ? "" : read.getContent();
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "Failed to read news details" : e.getMessage();
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
                var newsList = new ArrayList<NewsItem>();
                try {
                    for (var item : RuyiCli.listNewsItems(false)) {
                        boolean unread = !item.isRead();
                        newsList.add(new NewsItem(item.getTitle(), item.getId(), unread));
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "Failed to list news" : e.getMessage();
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
