package org.ruyisdk.news.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.ruyisdk.news.model.DataFetchService;
import org.ruyisdk.news.model.NewsItem;

/**
 * View model for the news list view.
 */
public class NewsListViewModel {
    private boolean isFetching = false;
    private String infoText = UpdatingState.notUpdated;

    private DataFetchService service;

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private final IObservableList<NewsItem> observableNewsList =
                    new WritableList<NewsItem>(new ArrayList<NewsItem>(), NewsItem.class);

    private class UpdatingState {
        private static final String notUpdated = "Not Updated, yet";
        private static final String updating = "Updating...";
        private static final String updatedTemplate = "Last Updated on %s";
    }

    /**
     * Creates a new view model.
     *
     * @param service the service used to fetch news
     */
    public NewsListViewModel(DataFetchService service) {
        this.service = service;
    }

    /**
     * Returns an observable list of news items.
     *
     * @return the observable news list
     */
    public IObservableList<NewsItem> getNewsList() {
        return observableNewsList;
    }

    // Add listener support methods
    /**
     * Adds a property change listener.
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Returns the current info text.
     *
     * @return the info text
     */
    public String getInfoText() {
        return infoText;
    }

    /**
     * Sets the info text.
     *
     * @param infoText the info text
     */
    public void setInfoText(String infoText) {
        changeSupport.firePropertyChange("infoText", this.infoText, this.infoText = infoText);
    }

    /**
     * Sets whether a fetch operation is in progress.
     *
     * @param isFetching whether a fetch operation is in progress
     */
    public void setFetching(boolean isFetching) {
        changeSupport.firePropertyChange("fetching", this.isFetching, this.isFetching = isFetching);
    }

    /**
     * Returns whether a fetch operation is in progress.
     *
     * @return whether a fetch operation is in progress
     */
    public boolean isFetching() {
        return isFetching;
    }

    /**
     * Triggers an asynchronous update of the news list.
     */
    public void onUpdateNewsList() {
        if (isFetching()) {
            return;
        }

        setFetching(true);
        setInfoText(UpdatingState.updating);

        service.fetchNewsListAsync(result -> {
            observableNewsList.getRealm().asyncExec(() -> {
                observableNewsList.clear();
                observableNewsList.addAll(result);
            });
            setInfoText(String.format(UpdatingState.updatedTemplate,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            setFetching(false);
        });
    }
}
