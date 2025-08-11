package org.ruyisdk.core.ruyi.model;

public class RepoConfig {
    private String name;
    private String url;
    private int priority; // 0 hightest
    private boolean state; // true false

    // 带参构造器
    public RepoConfig(String name, String url, int priority, boolean state) {
        this.name = name;
        this.url = url;
        this.priority = priority;
        this.state = state;
    }

    // Getter/Setter 方法
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
