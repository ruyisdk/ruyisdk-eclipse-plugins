package org.ruyisdk.core.ruyi.model;

public class RepoConfig {
  private String name;
  private String url;
  private int priority; // 0 indicates the highest priority
  private boolean state;

  public RepoConfig(String name, String url, int priority, boolean state) {
    this.name = name;
    this.url = url;
    this.priority = priority;
    this.state = state;
  }

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
