package org.ruyisdk.packages;

import org.eclipse.jface.viewers.LabelProvider;

/**
 * Label provider for tree nodes.
 */
public class TreeLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
        if (element instanceof TreeNode) {
            TreeNode node = (TreeNode) element;
            return node.getName() + (node.getDetails() != null ? " " + node.getDetails() : "");
        }
        return super.getText(element);
    }
}
