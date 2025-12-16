package org.ruyisdk.news.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A news item with title, id, unread status, and optional details.
 */
public class NewsItem {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String title;
    private String id;
    private Boolean unread;
    private String details = "";
    private Boolean detailsFetched = false;

    /**
     * Creates a news item.
     *
     * @param title the title
     * @param id the id
     * @param unread whether the item is unread
     */
    public NewsItem(String title, String id, Boolean unread) {
        this.title = title;
        this.id = id;
        this.unread = unread;
    }

    /**
     * Returns whether the item is unread.
     *
     * @return whether the item is unread
     */
    public Boolean getUnread() {
        return unread;
    }

    /**
     * Sets whether the item is unread.
     *
     * @param unread whether the item is unread
     */
    public void setUnread(Boolean unread) {
        pcs.firePropertyChange("unread", this.unread, this.unread = unread);
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        pcs.firePropertyChange("title", this.title, this.title = title);
    }

    /**
     * Returns the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(String id) {
        pcs.firePropertyChange("id", this.id, this.id = id);
    }

    /**
     * Returns the details text.
     *
     * @return the details text
     */
    public String getDetails() {
        return details;
    }

    /**
     * Sets the details text.
     *
     * @param details the details text
     */
    public void setDetails(String details) {
        pcs.firePropertyChange("details", this.details, this.details = details);
    }

    /**
     * Returns whether details have been fetched.
     *
     * @return whether details have been fetched
     */
    public Boolean getDetailsFetched() {
        return detailsFetched;
    }

    /**
     * Sets whether details have been fetched.
     *
     * @param detailsFetched whether details have been fetched
     */
    public void setDetailsFetched(Boolean detailsFetched) {
        this.detailsFetched = detailsFetched;
    }

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
}
