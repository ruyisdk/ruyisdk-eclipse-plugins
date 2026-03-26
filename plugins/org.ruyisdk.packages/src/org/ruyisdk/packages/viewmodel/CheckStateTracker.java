package org.ruyisdk.packages.viewmodel;

import java.util.HashMap;
import java.util.Map;
import org.ruyisdk.packages.model.TreeNode;

/**
 * Tracks user check-state overrides for tree nodes, separate from the domain model.
 *
 * <p>
 * When the user hasn't explicitly set a check state for a leaf, it defaults to the node's
 * downloaded (installed) state. Non-leaf nodes derive their checked/grayed state from their
 * descendants.
 */
public class CheckStateTracker {

    private final Map<TreeNode, Boolean> overrides = new HashMap<>();

    /** Record a user override for a leaf node. */
    public void setSelected(TreeNode node, boolean checked) {
        overrides.put(node, checked);
    }

    /** Clear all overrides (e.g. when a new tree is loaded). */
    public void clear() {
        overrides.clear();
    }

    /**
     * Returns whether the given node should appear checked.
     *
     * <p>
     * A leaf is checked when the user has explicitly selected it, or when there is no user override and
     * the package is already downloaded. A non-leaf is checked when at least one descendant leaf is
     * effectively checked.
     */
    public boolean isEffectivelyChecked(TreeNode node) {
        if (node.isLeaf()) {
            final var override = overrides.get(node);
            return override != null ? override : node.isDownloaded();
        }
        for (final var child : node.getChildren()) {
            if (isEffectivelyChecked(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given non-leaf node should appear grayed (partially checked).
     *
     * <p>
     * A node is grayed when some, but not all, of its descendant leaves are effectively checked. Leaf
     * nodes are never grayed.
     */
    public boolean isEffectivelyGrayed(TreeNode node) {
        if (node.isLeaf()) {
            return false;
        }
        final var state = new boolean[] {false /* hasCheckedLeaf */, false /* hasUncheckedLeaf */};
        collectLeafStates(node, state);
        return state[0] && state[1];
    }

    /** Pre-order traversal to collect leaf states. */
    private void collectLeafStates(TreeNode node, boolean[] state) {
        if (node.isLeaf()) {
            if (isEffectivelyChecked(node)) {
                state[0] = true; // hasCheckedLeaf
            } else {
                state[1] = true; // hasUncheckedLeaf
            }
            return;
        }
        for (final var child : node.getChildren()) {
            collectLeafStates(child, state);
            if (state[0] && state[1]) {
                return; // both found, no need to continue
            }
        }
    }
}
