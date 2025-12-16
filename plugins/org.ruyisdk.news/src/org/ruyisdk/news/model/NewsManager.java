package org.ruyisdk.news.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a list of news items and notifies listeners on changes.
 */
public class NewsManager {
    private List<NewsItem> newsList;
    private PropertyChangeSupport propertyChangeSupport;

    /**
     * Creates a new {@link NewsManager}.
     */
    public NewsManager() {
        newsList = new ArrayList<>();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Adds a property change listener.
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Adds a news item.
     *
     * @param news the news item
     */
    public void addNews(NewsItem news) {
        newsList.add(news);
        propertyChangeSupport.firePropertyChange("newsList", null, newsList);
    }

    /**
     * Removes a news item.
     *
     * @param news the news item
     */
    public void removeNews(NewsItem news) {
        newsList.remove(news);
        propertyChangeSupport.firePropertyChange("newsList", null, newsList);
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
            propertyChangeSupport.firePropertyChange("newsList", null, newsList);
        }
    }

    /**
     * Adds a listener for news list changes.
     *
     * @param taskListener the listener
     */
    public void addNewsListener(NewsListener taskListener) {
        // TODO Auto-generated method stub
        propertyChangeSupport.addPropertyChangeListener(taskListener);
    }

    /**
     * A listener for news list changes.
     */
    public static class NewsListener implements PropertyChangeListener {
        /** {@inheritDoc} */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // TODO Auto-generated method stub

        }
    }
}
