package org.ruyisdk.core.ruyi.model;

/**
 * Represents a repository configuration for RuyiSDK package management.
 *
 * <p>This class stores information about a package repository including its name,
 * URL, priority level, and enabled state.
 */
public class RepoConfig {
    /** The display name of the repository. */
    private String name;
    
    /** The URL endpoint of the repository. */
    private String url;
    
    /** 
     * The priority level of the repository (0 is highest priority).
     * Lower values indicate higher priority.
     */
    private int priority;
    
    /** The enabled state of the repository (true = enabled, false = disabled). */
    private boolean state;

    /**
     * Constructs a new repository configuration.
     *
     * @param name the display name of the repository
     * @param url the repository endpoint URL
     * @param priority the priority level (0 is highest)
     * @param state the initial enabled state
     */
    public RepoConfig(String name, String url, int priority, boolean state) {
        this.name = name;
        this.url = url;
        this.priority = priority;
        this.state = state;
    }

    // Basic getters and setters (excluded from documentation per request)
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getPriority() {
        return priority;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean getCheckState() {
        return state;
    }
}