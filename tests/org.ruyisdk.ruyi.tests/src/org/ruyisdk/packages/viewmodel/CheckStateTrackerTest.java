package org.ruyisdk.packages.viewmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.ruyisdk.packages.model.TreeNode;

/**
 * Unit tests for {@link CheckStateTracker}.
 */
public class CheckStateTrackerTest {

    // ------------------------------------------------------------------
    // Leaf inheritance and overrides
    // ------------------------------------------------------------------

    /**
     * Leaf nodes should inherit their effective checked state from the
     * downloaded flag until the user explicitly overrides them.
     */
    @Test
    public void leafInheritsDownloadedUntilOverridden() {
        TreeNode leaf = createLeaf("leaf");
        CheckStateTracker tracker = new CheckStateTracker();

        // Initially nothing is downloaded or user-selected.
        assertFalse("Leaf should not be checked by default", tracker.isEffectivelyChecked(leaf));
        assertFalse("Leaf should not be grayed by default", tracker.isEffectivelyGrayed(leaf));

        // When the leaf is downloaded, it becomes effectively checked.
        leaf.setDownloaded(true);
        assertTrue("Downloaded leaf should be checked", tracker.isEffectivelyChecked(leaf));
        assertFalse("Downloaded leaf should not be grayed", tracker.isEffectivelyGrayed(leaf));

        // A user override to unchecked should take precedence over downloaded.
        tracker.setSelected(leaf, false);
        assertFalse("User override should uncheck downloaded leaf", tracker.isEffectivelyChecked(leaf));
        assertFalse("Explicitly unchecked leaf should not be grayed", tracker.isEffectivelyGrayed(leaf));

        // A user override to checked should keep it checked even if not downloaded.
        leaf.setDownloaded(false);
        tracker.setSelected(leaf, true);
        assertTrue("User override should check non-downloaded leaf", tracker.isEffectivelyChecked(leaf));
        assertFalse("Explicitly checked leaf should not be grayed", tracker.isEffectivelyGrayed(leaf));
    }

    // ------------------------------------------------------------------
    // Parent states for child combinations
    // ------------------------------------------------------------------

    /**
     * Parent checked / grayed state should reflect the aggregate state of its
     * children for all-checked, all-unchecked, and mixed combinations.
     */
    @Test
    public void parentStateForChildCombinations() {
        TreeNode child1 = createLeaf("child1");
        TreeNode child2 = createLeaf("child2");
        TreeNode parent = new TreeNode("parent", null);
        parent.addChild(child1);
        parent.addChild(child2);

        CheckStateTracker tracker = new CheckStateTracker();

        // All children unchecked -> parent unchecked and not grayed.
        assertFalse(tracker.isEffectivelyChecked(parent));
        assertFalse(tracker.isEffectivelyGrayed(parent));

        // All children checked (via downloaded) -> parent checked and not grayed.
        child1.setDownloaded(true);
        child2.setDownloaded(true);
        assertTrue("Parent should be checked when all children are checked",
                        tracker.isEffectivelyChecked(parent));
        assertFalse("Parent should not be grayed when all children are checked",
                        tracker.isEffectivelyGrayed(parent));

        // Mixed children -> parent checked (at least one) but grayed.
        tracker.setSelected(child1, false);
        assertTrue("Parent should be checked when at least one child is checked",
                        tracker.isEffectivelyChecked(parent));
        assertTrue("Parent should be grayed when children are mixed",
                        tracker.isEffectivelyGrayed(parent));

        // All children explicitly unchecked -> parent unchecked and not grayed.
        tracker.setSelected(child2, false);
        assertFalse("Parent should be unchecked when all children are unchecked",
                        tracker.isEffectivelyChecked(parent));
        assertFalse("Parent should not be grayed when all children are unchecked",
                        tracker.isEffectivelyGrayed(parent));
    }

    // ------------------------------------------------------------------
    // Deep tree gray-state propagation
    // ------------------------------------------------------------------

    /**
     * In deeper trees, a mixed state in a subtree should propagate a grayed
     * state up through all ancestors.
     */
    @Test
    public void grayStatePropagatesToAncestors() {
        TreeNode leaf1 = createLeaf("leaf1");
        TreeNode leaf2 = createLeaf("leaf2");
        TreeNode mid = new TreeNode("mid", null);
        mid.addChild(leaf1);
        mid.addChild(leaf2);
        TreeNode root = new TreeNode("root", null);
        root.addChild(mid);

        CheckStateTracker tracker = new CheckStateTracker();

        // Make the mid subtree mixed: one child checked, one unchecked.
        leaf1.setDownloaded(true);

        // Mid should be grayed due to mixed children.
        assertTrue("Mid should be grayed when its children are mixed",
                        tracker.isEffectivelyGrayed(mid));
        assertTrue("Mid should be checked (at least one child checked)",
                        tracker.isEffectivelyChecked(mid));

        // Root has a single grayed child -> it should also be grayed.
        assertTrue("Root should be grayed when a descendant subtree is mixed",
                        tracker.isEffectivelyGrayed(root));

        // When both leaves become checked, gray state should clear from ancestors.
        leaf2.setDownloaded(true);
        assertFalse("Mid should not be grayed when all children are checked",
                        tracker.isEffectivelyGrayed(mid));
        assertTrue("Mid should be checked when all children are checked",
                        tracker.isEffectivelyChecked(mid));

        assertFalse("Root should not be grayed when all descendants are checked",
                        tracker.isEffectivelyGrayed(root));
        assertTrue("Root should be checked when all descendants are checked",
                        tracker.isEffectivelyChecked(root));
    }

    // ------------------------------------------------------------------
    // Clear resets overrides
    // ------------------------------------------------------------------

    @Test
    public void clearResetsOverrides() {
        TreeNode leaf = createLeaf("leaf");
        CheckStateTracker tracker = new CheckStateTracker();

        tracker.setSelected(leaf, true);
        assertTrue(tracker.isEffectivelyChecked(leaf));

        tracker.clear();
        // After clear, the leaf should fall back to its downloaded state (false).
        assertFalse(tracker.isEffectivelyChecked(leaf));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static TreeNode createLeaf(String name) {
        TreeNode node = new TreeNode(name, null);
        node.setLeaf(true);
        return node;
    }
}
