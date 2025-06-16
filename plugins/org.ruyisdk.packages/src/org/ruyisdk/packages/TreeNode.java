package org.ruyisdk.packages;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private String name;
    private String details;
    private String installCommand;
    private boolean selected; // Record whether the checkbox is selected
    private boolean isLeaf; // Mark whether it is the last node
    private List<TreeNode> children;
    private boolean downloaded = false;// Download mark field

    public TreeNode(String name, String details) {
        this(name, details, null);
    }

    public TreeNode(String name, String details, String installCommand) {
        this.name = name;
        this.details = details;
        this.installCommand = installCommand;
        this.children = new ArrayList<>();
        this.selected = false; // Default not selected
        this.isLeaf = false; // Default not a leaf node
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return details;
    }

    public String getInstallCommand() {
        return installCommand;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void addChild(TreeNode child) {
        children.add(child);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
    public boolean isDownloaded() { return downloaded; }
    public void setDownloaded(boolean downloaded) { this.downloaded = downloaded; }
}