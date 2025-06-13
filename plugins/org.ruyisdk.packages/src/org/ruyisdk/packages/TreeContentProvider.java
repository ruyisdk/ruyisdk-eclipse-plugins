package org.ruyisdk.packages;

import org.eclipse.jface.viewers.ITreeContentProvider;

public class TreeContentProvider implements ITreeContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof TreeNode) {
            return ((TreeNode) inputElement).getChildren().toArray();
        }
        return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof TreeNode) {
            return ((TreeNode) parentElement).getChildren().toArray();
        }
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        return null; 
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof TreeNode) {
            return ((TreeNode) element).hasChildren();
        }
        return false;
    }
}
