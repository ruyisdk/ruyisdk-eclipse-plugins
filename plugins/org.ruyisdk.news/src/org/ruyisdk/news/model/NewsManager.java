package org.ruyisdk.news.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a list of news items and notifies listeners on changes.
 */
public class NewsManager {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final List<NewsItem> newsList = new ArrayList<>();

    /**
     * Adds a property change listener.
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Adds a news item.
     *
     * @param news the news item
     */
    public void addNews(NewsItem news) {
        newsList.add(news);
        pcs.firePropertyChange("newsList", null, newsList);
    }

    /**
     * Removes a news item.
     *
     * @param news the news item
     */
    public void removeNews(NewsItem news) {
        newsList.remove(news);
        pcs.firePropertyChange("newsList", null, newsList);
    }

    /**
     * Returns a copy of the current news list.
     *
     * @return a copy of the news list
     */
    public List<NewsItem> getNewsList() {
        return new ArrayList<>(newsList);
    }

    /**
     * Replaces an existing news item with an updated one.
     *
     * @param oldNews the existing item
     * @param newNews the updated item
     */
    public void updateNews(NewsItem oldNews, NewsItem newNews) {
        int index = newsList.indexOf(oldNews);
        if (index != -1) {
            newsList.set(index, newNews);
            pcs.firePropertyChange("newsList", null, newsList);
        }
    }
}
