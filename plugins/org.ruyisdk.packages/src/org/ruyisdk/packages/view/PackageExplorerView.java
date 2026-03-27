package org.ruyisdk.packages.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.ruyisdk.packages.model.TreeNode;
import org.ruyisdk.packages.viewmodel.DeviceSelectionViewModel;
import org.ruyisdk.packages.viewmodel.PackageExplorerViewModel;
import org.ruyisdk.packages.viewmodel.PackageOperationViewModel;

/**
 * View for exploring packages (MVVM pattern, View layer).
 *
 * <p>
 * All mutable state lives in {@link PackageExplorerViewModel}; this class is responsible only for
 * widget creation, event wiring, and reacting to property-change events fired by the ViewModel.
 */
public class PackageExplorerView extends ViewPart {

    private PackageExplorerViewModel viewModel;
    private PropertyChangeListener viewModelListener;

    private CheckboxTreeViewer treeViewer;
    private Link deviceInfoLink;
    private Text infoText;
    private IToolBarManager toolBar;

    @Override
    public void createPartControl(Composite parent) {
        viewModel = new PackageExplorerViewModel(Display.getDefault()::asyncExec);

        parent.setLayout(new GridLayout(1, false));

        // Populate the view toolbar with actions and device info
        createToolbarActions(parent.getShell());

        // SashForm splits the view horizontally: tree on left, info pane on right
        final var sashForm = new SashForm(parent, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createTreeView(sashForm);

        // Read-only info pane on the right
        infoText = new Text(sashForm, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        infoText.setText(viewModel.getInfoPaneText());

        sashForm.setWeights(new int[] {70, 30});

        // Bind ViewModel property changes to widget updates
        viewModelListener = this::onViewModelChanged;
        viewModel.addPropertyChangeListener(viewModelListener);

        // Kick off initial data loading
        viewModel.initialize();
    }

    /** React to ViewModel property change, all events arrive on the UI thread. */
    private void onViewModelChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case PackageExplorerViewModel.PROP_PACKAGE_ROOT:
                final var root = viewModel.getPackageRoot();
                treeViewer.setInput(root);
                treeViewer.refresh();
                if (root != null) {
                    expandTree(root);
                }
                break;
            case PackageExplorerViewModel.PROP_DEVICE_INFO_TEXT:
                if (deviceInfoLink != null && !deviceInfoLink.isDisposed()) {
                    deviceInfoLink.setText(viewModel.getDeviceInfoText());
                    toolBar.update(true);
                    getViewSite().getActionBars().updateActionBars();
                }
                break;
            case PackageExplorerViewModel.PROP_INFO_PANE_TEXT:
                if (infoText != null && !infoText.isDisposed()) {
                    infoText.setText(viewModel.getInfoPaneText());
                }
                break;
            case PackageExplorerViewModel.PROP_ERROR:
                final var msg = (String) evt.getNewValue();
                if (msg != null) {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", msg);
                }
                break;
            default:
                break;
        }
    }

    private void createToolbarActions(Shell parentShell) {
        toolBar = getViewSite().getActionBars().getToolBarManager();

        // Clickable device info link in the toolbar, opens the device selection dialog
        toolBar.add(new ControlContribution("deviceInfo") {
            @Override
            protected Control createControl(Composite parent) {
                final var wrapper = new Composite(parent, SWT.NONE);
                {
                    final var gridLayout = new GridLayout(1, false);
                    gridLayout.marginHeight = 0;
                    gridLayout.marginWidth = 0;
                    wrapper.setLayout(gridLayout);
                }

                deviceInfoLink = new Link(wrapper, SWT.NONE);
                deviceInfoLink.setText(viewModel.getDeviceInfoText());
                deviceInfoLink.setToolTipText("Click to select a different device");
                deviceInfoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
                deviceInfoLink.addListener(SWT.Selection, event -> openDeviceSelectionDialog(parentShell));

                return wrapper;
            }
        });

        toolBar.add(new Separator());

        final var refreshAction = new Action("Refresh List") {
            @Override
            public void run() {
                viewModel.refreshPackages();
            }
        };
        refreshAction.setToolTipText("Refresh the package list");
        toolBar.add(refreshAction);

        toolBar.add(new Separator());

        final var openPkgDirAction = new Action("Open Pkg Dir") {
            @Override
            public void run() {
                if (!Program.launch(viewModel.getPackageDownloadDir())) {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                                    "Cannot open compressed package download directory.");
                }
            }
        };
        openPkgDirAction.setToolTipText("Open the compressed package download directory");
        toolBar.add(openPkgDirAction);

        toolBar.add(new Separator());

        final var openImagesDirAction = new Action("Open Images Dir") {
            @Override
            public void run() {
                if (!Program.launch(viewModel.getImagesDownloadDir())) {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                                    "Cannot open image files download directory.");
                }
            }
        };
        openImagesDirAction.setToolTipText("Open the image files download directory");
        toolBar.add(openImagesDirAction);

        toolBar.add(new Separator());

        final var packageOperationAction = new Action("Install / Uninstall Packages") {
            @Override
            public void run() {
                performPackageOperations();
            }
        };
        packageOperationAction.setToolTipText("Install checked packages and uninstall unchecked installed packages");
        toolBar.add(packageOperationAction);

        getViewSite().getActionBars().updateActionBars();
    }

    private void openDeviceSelectionDialog(Shell parentShell) {
        final var dialogVm = new DeviceSelectionViewModel(viewModel.getDevices(), viewModel.getChosenDevice(),
                        viewModel.getDeviceListErrorMessage());
        final var dialog = new DeviceSelectionDialog(parentShell, dialogVm,
                        (device, onDone) -> viewModel.setChosenDeviceAndReload(device, onDone));
        dialog.open();
    }

    private void performPackageOperations() {
        final var operations = viewModel.collectPendingOperations();
        if (operations.isEmpty()) {
            ScrollableMessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info",
                            "No changes to apply. Check packages to install, "
                                            + "or uncheck installed packages to uninstall.");
            return;
        }

        final var message = viewModel.getConfirmationMessage(operations);
        if (!ScrollableMessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Confirm Changes", message)) {
            return;
        }

        final var operationVm = new PackageOperationViewModel(Display.getDefault()::asyncExec, operations,
                        viewModel::refreshPackages);
        final var workbenchShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final var dialog = new PackageOperationDialog(workbenchShell, operationVm);
        dialog.open();
    }

    private void createTreeView(Composite parent) {
        treeViewer = new CheckboxTreeViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

        treeViewer.setContentProvider(new ITreeContentProvider() {
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
                if (element instanceof TreeNode) {
                    return ((TreeNode) element).getParent();
                }
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                if (element instanceof TreeNode) {
                    return ((TreeNode) element).hasChildren();
                }
                return false;
            }
        });

        treeViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof TreeNode node) {
                    return node.getName() + (node.getDetails() != null ? " " + node.getDetails() : "");
                }
                return super.getText(element);
            }
        });

        treeViewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element) {
                if (element instanceof TreeNode node) {
                    return viewModel.isNodeChecked(node);
                }
                return false;
            }

            @Override
            public boolean isGrayed(Object element) {
                if (element instanceof TreeNode node) {
                    return viewModel.isNodeGrayed(node);
                }
                return false;
            }
        });

        treeViewer.addSelectionChangedListener(event -> {
            final var selection = (StructuredSelection) treeViewer.getSelection();
            final var node = (TreeNode) selection.getFirstElement();
            viewModel.updateSelectedNode(node);
        });

        treeViewer.addCheckStateListener(event -> {
            final var node = (TreeNode) event.getElement();
            viewModel.setNodeChecked(node, event.getChecked());
            treeViewer.refresh();
        });
    }

    private void expandTree(TreeNode root) {
        // Unconditionally expand the root's immediate children to make the view full
        treeViewer.expandToLevel(root, 2);

        // Then recursively expand only checked subtrees
        if (root.getChildren() != null) {
            for (final var child : root.getChildren()) {
                expandCheckedSubtree(child);
            }
        }
    }

    private void expandCheckedSubtree(TreeNode node) {
        if (node.isLeaf() || node.getChildren() == null) {
            return;
        }
        if (viewModel.isNodeChecked(node)) {
            treeViewer.expandToLevel(node, 1);
            for (final var child : node.getChildren()) {
                expandCheckedSubtree(child);
            }
        }
    }

    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        if (viewModel != null && viewModelListener != null) {
            viewModel.removePropertyChangeListener(viewModelListener);
        }
        super.dispose();
    }
}
