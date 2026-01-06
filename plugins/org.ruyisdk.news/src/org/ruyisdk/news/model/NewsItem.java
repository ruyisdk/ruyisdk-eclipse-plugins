package org.ruyisdk.news.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A news item with ordinal/index, title, id, unread status, and details.
 */
public class NewsItem {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private int ord;
    private String title;
    private String id;
    private boolean unread;
    private String details = "";
    private String detailsHtml = "";
    private boolean detailsFetched = false;

    /**
     * Creates a news item.
     *
     * @param ord the ordinal/index
     * @param title the title
     * @param id the id
     * @param unread whether the item is unread
     */
    public NewsItem(int ord, String title, String id, boolean unread) {
        this.title = title;
        this.id = id;
        this.unread = unread;
        this.ord = ord;
    }

    /**
     * Returns the ordinal/index.
     *
     * @return the ordinal/index
     */
    public int getOrd() {
        return ord;
    }

    /**
     * Sets the ordinal/index.
     *
     * @param ord the ordinal/index
     */
    public void setOrd(int ord) {
        pcs.firePropertyChange("ord", this.ord, this.ord = ord);
    }

    /**
     * Returns whether the item is unread.
     *
     * @return whether the item is unread
     */
    public boolean getUnread() {
        return unread;
    }

    /**
     * Sets whether the item is unread.
     *
     * @param unread whether the item is unread
     */
    public void setUnread(boolean unread) {
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
     * Returns the details as rendered HTML.
     *
     * @return the details HTML
     */
    public String getDetailsHtml() {
        return detailsHtml;
    }

    /**
     * Sets the details HTML.
     *
     * @param detailsHtml the details HTML
     */
    public void setDetailsHtml(String detailsHtml) {
        pcs.firePropertyChange("detailsHtml", this.detailsHtml, this.detailsHtml = detailsHtml);
    }

    /**
     * Returns whether details have been fetched.
     *
     * @return whether details have been fetched
     */
    public boolean getDetailsFetched() {
        return detailsFetched;
    }

    /**
     * Sets whether details have been fetched.
     *
     * @param detailsFetched whether details have been fetched
     */
    public void setDetailsFetched(boolean detailsFetched) {
        pcs.firePropertyChange("detailsFetched", this.detailsFetched, this.detailsFetched = detailsFetched);
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
