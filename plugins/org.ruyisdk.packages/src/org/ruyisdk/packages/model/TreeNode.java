package org.ruyisdk.packages.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the package tree.
 */
public class TreeNode {
    private String name;
    private String details;
    private String packageRef;
    private boolean isLeaf; // Mark whether it is the last node
    private List<TreeNode> children;
    private TreeNode parent;
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
     * @param packageRef package reference
     */
    public TreeNode(String name, String details, String packageRef) {
        this.name = name;
        this.details = details;
        this.packageRef = packageRef;
        this.children = new ArrayList<>();
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
     * Gets the package reference.
     *
     * @return package reference
     */
    public String getPackageRef() {
        return packageRef;
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
        child.parent = this;
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
     * Gets the parent node.
     *
     * @return parent node, or null if this is the root
     */
    public TreeNode getParent() {
        return parent;
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
