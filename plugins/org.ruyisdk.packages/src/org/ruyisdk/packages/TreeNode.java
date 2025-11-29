package org.ruyisdk.packages;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the package tree.
 */
public class TreeNode {
    private String name;
    private String details;
    private String installCommand;
    private boolean selected; // Record whether the checkbox is selected
    private boolean isLeaf; // Mark whether it is the last node
    private List<TreeNode> children;
    private boolean downloaded = false; // Download mark field

    /**
     * Constructs a tree node.
     *
     * @param name node name
     * @param details node details
     */
    public TreeNode(String name, String details) {
        this(name, details, null);
    }

    /**
     * Constructs a tree node with install command.
     *
     * @param name node name
     * @param details node details
     * @param installCommand install command
     */
    public TreeNode(String name, String details, String installCommand) {
        this.name = name;
        this.details = details;
        this.installCommand = installCommand;
        this.children = new ArrayList<>();
        this.selected = false; // Default not selected
        this.isLeaf = false; // Default not a leaf node
    }

    /**
     * Gets the node name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the details.
     *
     * @return details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Gets the install command.
     *
     * @return install command
     */
    public String getInstallCommand() {
        return installCommand;
    }

    /**
     * Checks if selected.
     *
     * @return true if selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selected state.
     *
     * @param selected selected state
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Checks if this is a leaf node.
     *
     * @return true if leaf
     */
    public boolean isLeaf() {
        return isLeaf;
    }

    /**
     * Sets the leaf state.
     *
     * @param isLeaf leaf state
     */
    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    /**
     * Gets child nodes.
     *
     * @return children
     */
    public List<TreeNode> getChildren() {
        return children;
    }

    /**
     * Adds a child node.
     *
     * @param child child node
     */
    public void addChild(TreeNode child) {
        children.add(child);
    }

    /**
     * Checks if has children.
     *
     * @return true if has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Checks if downloaded.
     *
     * @return true if downloaded
     */
    public boolean isDownloaded() {
        return downloaded;
    }

    /**
     * Sets the downloaded state.
     *
     * @param downloaded downloaded state
     */
    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }
}
