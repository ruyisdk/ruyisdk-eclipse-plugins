package org.ruyisdk.packages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.part.ViewPart;
import org.ruyisdk.packages.JsonParser;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * View for exploring packages.
 */
public class PackageExplorerView extends ViewPart {
    private CheckboxTreeViewer viewer;
    private String chosenType;

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        chosenType = showHardwareTypeSelectionDialog(parent.getShell());
        if (chosenType != null) {
            refreshList();
        }
        // build button docker
        Composite buttonComposite = new Composite(parent, SWT.NONE);
        // Removed one button, so grid layout is now 5 columns
        buttonComposite.setLayout(new GridLayout(5, false));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // add refresh button
        Button refreshButton = new Button(buttonComposite, SWT.PUSH);
        refreshButton.setText("Refresh List");
        refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        refreshButton.addListener(SWT.Selection, event -> refreshList());

        // add openDownloadDirectory button
        Button openDirButton = new Button(buttonComposite, SWT.PUSH);
        openDirButton.setText("Open Pkg Dir");
        openDirButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        openDirButton.addListener(SWT.Selection, event -> {
            try {
                String cacheHome = System.getenv("XDG_CACHE_HOME");
                String downloadDir;
                if (cacheHome != null && !cacheHome.isEmpty()) {
                    downloadDir = cacheHome + "/ruyi/distfiles";
                } else {
                    downloadDir = System.getProperty("user.home") + "/.cache/ruyi/distfiles";
                }
                Runtime.getRuntime().exec(new String[] {"xdg-open", downloadDir});
            } catch (IOException e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                                "Cannot open compressed package download directory: " + e.getMessage());
            }
        });

        // add open imageDirectory button
        Button openBlobsButton = new Button(buttonComposite, SWT.PUSH);
        openBlobsButton.setText("Open Images Dir");
        openBlobsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        openBlobsButton.addListener(SWT.Selection, event -> {
            try {
                String dataHome = System.getenv("XDG_DATA_HOME");
                String blobsDir;
                if (dataHome != null && !dataHome.isEmpty()) {
                    blobsDir = dataHome + "/ruyi/blobs";
                } else {
                    blobsDir = System.getProperty("user.home") + "/.local/share/ruyi/blobs";
                }
                Runtime.getRuntime().exec(new String[] {"xdg-open", blobsDir});
            } catch (IOException e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                                "Cannot open image files download directory: " + e.getMessage());
            }
        });

        // add download button
        Button downloadButton = new Button(buttonComposite, SWT.PUSH);
        downloadButton.setText("Download");
        downloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        downloadButton.addListener(SWT.Selection, event -> {
            Object[] checkedElements = viewer.getCheckedElements();
            List<TreeNode> selectedNodes = new ArrayList<>();
            for (Object obj : checkedElements) { // Download Monitor
                if (obj instanceof TreeNode) {
                    TreeNode node = (TreeNode) obj;
                    if (node.isLeaf() && !node.isDownloaded()) {
                        selectedNodes.add(node);
                    }
                }
            }
            if (selectedNodes.isEmpty()) {
                MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Point:", "No files selected!");
                return;
            }
            boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Confirm Download",
                            "Are you sure you want to download the selected files?");
            if (confirmed) {
                for (TreeNode node : selectedNodes) {
                    modifyPackage(node.getPackageRef(), false);
                }
            }
        });

        // add switch board button
        Button switchBoardButton = new Button(buttonComposite, SWT.PUSH);
        switchBoardButton.setText("Select Development Board");
        switchBoardButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        switchBoardButton.addListener(SWT.Selection, event -> {
            String newType = showHardwareTypeSelectionDialog(parent.getShell());
            if (newType != null && !newType.equals(chosenType)) {
                chosenType = newType;
                refreshList();
            }
        });

        viewer = new CheckboxTreeViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        Tree tree = viewer.getTree();
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.setContentProvider(new TreeContentProvider());
        viewer.setLabelProvider(new TreeLabelProvider());

        // Only let leaf nodes have checkboxes
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element) {
                return false;
            }

            @Override
            public boolean isGrayed(Object element) {

                if (element instanceof TreeNode) {
                    return !((TreeNode) element).isLeaf();
                }
                return false;
            }
        });

        // Prevent non-leaf nodes from being selected
        viewer.addCheckStateListener(event -> {
            Object element = event.getElement();
            if (element instanceof TreeNode && !((TreeNode) element).isLeaf()) {
                viewer.setChecked(element, false);
            }
        });

        // Create and register the context menu
        createContextMenu();

        Display.getDefault().asyncExec(() -> {
            try {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void fillContextMenu(IMenuManager manager) {
        IStructuredSelection selection = viewer.getStructuredSelection();
        Object firstElement = selection.getFirstElement();

        if (firstElement instanceof TreeNode) {
            TreeNode node = (TreeNode) firstElement;
            // Only show the uninstall option for downloaded leaf nodes
            if (node.isLeaf() && node.isDownloaded()) {
                Action uninstallAction = new Action("Uninstall") {
                    @Override
                    public void run() {
                        boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
                                        "Confirm Uninstall", "Are you sure you want to uninstall the selected package '"
                                                        + node.getName() + "'?");

                        if (confirmed) {
                            modifyPackage(node.getPackageRef(), true);
                        }
                    }
                };
                manager.add(uninstallAction);
            }
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private void loadPackagesAsync() {
        final var job = Job.create("Loading packages", monitor -> {
            if (monitor.isCanceled()) {
                return;
            }
            try {
                final var entity = "device:" + chosenType;
                if (monitor.isCanceled()) {
                    return;
                }
                final var output = RuyiCli.listRelatedToEntity(entity);
                if (monitor.isCanceled()) {
                    return;
                }

                StringBuilder outputBuilder = new StringBuilder();
                outputBuilder.append("[");
                boolean first = true;
                final var lines = output.split("\\R");
                for (final var line : lines) {
                    if (monitor.isCanceled()) {
                        return;
                    }
                    final var trimmedLine = line.trim();
                    // Only concatenate packet data and filter logs
                    if (!trimmedLine.isEmpty() && trimmedLine.startsWith("{")
                                    && !trimmedLine.contains("\"ty\":\"log-v1\"")) {
                        if (!first) {
                            outputBuilder.append(",");
                        }
                        outputBuilder.append(trimmedLine);
                        first = false;
                    }
                }
                outputBuilder.append("]");
                String jsonData = outputBuilder.toString();

                Display.getDefault().asyncExec(() -> {
                    try {
                        TreeNode root = JsonParser.parseJson(jsonData, chosenType);
                        viewer.setInput(root);
                        viewer.expandAll();
                        markDownloadedNodes(root);
                    } catch (Exception e) {
                        MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                                        "Failed to parse JSON data: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                                    "Failed to execute command: " + e.getMessage());
                });
            }
        });
        job.setUser(true);
        job.setPriority(Job.LONG);
        job.schedule();
    }

    // Recursively mark downloaded nodes
    private void markDownloadedNodes(TreeNode node) {
        if (node.isLeaf() && node.isDownloaded()) {
            viewer.setChecked(node, true);
            viewer.setGrayed(node, true);
        }
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                markDownloadedNodes(child);
            }
        }
    }

    private void modifyPackage(String packageRef, boolean uninstall) {
        Display.getDefault().asyncExec(() -> {
            final var dialog = new OutputLiveDialog(Display.getDefault().getActiveShell(), packageRef, uninstall);
            dialog.open();
        });
    }

    // Real-time output dialog
    class OutputLiveDialog extends Dialog {
        private String packageRef;
        private boolean uninstall;
        private Text text;

        public OutputLiveDialog(Shell parentShell, String packageRef, boolean uninstall) {
            super(parentShell);
            this.packageRef = packageRef;
            this.uninstall = uninstall;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));
            text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            text.setText((uninstall ? "Uninstalling" : "Installing") + " \"" + packageRef + "\"\n\nOutput:\n");
            startCommand();
            return container;
        }

        private void startCommand() {
            final var job = Job.create((uninstall ? "Uninstalling" : "Installing") + " " + packageRef, monitor -> {
                if (monitor.isCanceled()) {
                    return;
                }
                try {
                    final Consumer<String> lineCallback = line -> {
                        if (monitor.isCanceled()) {
                            return;
                        }
                        Display.getDefault().asyncExec(() -> {
                            if (text != null && !text.isDisposed()) {
                                text.append(line + "\n");
                            }
                        });
                    };
                    if (uninstall) {
                        RuyiCli.uninstallPackageStreaming(packageRef, true, lineCallback, null);
                    } else {
                        RuyiCli.installPackageStreaming(packageRef, lineCallback, null);
                    }
                    Display.getDefault().asyncExec(() -> refreshList());
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        if (text != null && !text.isDisposed()) {
                            text.append("Failed to execute the command: " + e.getMessage() + "\n");
                        }
                    });
                }
            });
            job.setUser(true);
            job.schedule();
        }

        @Override
        protected org.eclipse.swt.graphics.Point getInitialSize() {
            return new org.eclipse.swt.graphics.Point(600, 400);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, OK, "OK", true); // Only create "OK" button
        }
    }

    // Custom dialog
    class OutputDialog extends Dialog {
        private String content;

        public OutputDialog(Shell parentShell, String content) {
            super(parentShell);
            this.content = content;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));
            Text text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            text.setText(content);
            return container;
        }

        @Override
        protected org.eclipse.swt.graphics.Point getInitialSize() {
            // Set the initial size of the dialog
            return new org.eclipse.swt.graphics.Point(600, 400);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, OK, "OK", true); // Only create "OK" button
        }
    }

    private void refreshList() {
        if (chosenType == null || chosenType.isEmpty()) {
            return;
        }
        loadPackagesAsync();
    }

    private String[] fetchHardwareEntities() {
        List<String> entityIds = new ArrayList<>();

        try {
            final var output = RuyiCli.listEntitiesByType("device").replace("\n", "").replace("\r", "");
            entityIds = JsonParser.parseAllEntityIdsInOneLine(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entityIds.toArray(new String[0]);
    }

    private String showHardwareTypeSelectionDialog(Shell shell) {
        String[] hardwareTypes = fetchHardwareEntities();
        // add no res found check
        if (hardwareTypes == null || hardwareTypes.length == 0) {
            String msg = "Could not find any supported development board entities. "
                            + "Please check your Ruyi installation.";
            MessageDialog.openWarning(shell, "No Hardware Found", msg);
            return null;
        }

        HardwareSelectionDialog dialog = new HardwareSelectionDialog(shell, hardwareTypes);
        if (dialog.open() == Dialog.OK) {
            return dialog.getSelectedHardwareType();
        }
        return null; // User cancelled
    }

    // Custom Dialog for selecting hardware type
    class HardwareSelectionDialog extends Dialog {
        private org.eclipse.swt.widgets.List listWidget;
        private String[] hardwareTypes;
        private String selectedHardwareType;
        private String title;

        public HardwareSelectionDialog(Shell parentShell, String[] hardwareTypes) {
            super(parentShell);
            this.hardwareTypes = hardwareTypes;
            this.title = "Select Development Board";
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(title);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));

            // Create the List widget
            listWidget = new org.eclipse.swt.widgets.List(container, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            listWidget.setLayoutData(gd);

            // Populate the list
            if (hardwareTypes != null) {
                listWidget.setItems(hardwareTypes);
            }

            // Add a listener for double-click to select and close
            listWidget.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                @Override
                public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
                    okPressed();
                }
            });

            return container;
        }

        @Override
        protected void okPressed() {
            String[] selection = listWidget.getSelection();
            if (selection.length > 0) {
                selectedHardwareType = selection[0];
            }
            super.okPressed();
        }

        public String getSelectedHardwareType() {
            return selectedHardwareType;
        }

        @Override
        protected org.eclipse.swt.graphics.Point getInitialSize() {
            return new org.eclipse.swt.graphics.Point(450, 300);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            // Create a "Select" button that triggers okPressed()
            createButton(parent, OK, "Select", true);
            // Create a standard "Cancel" button
            createButton(parent, CANCEL, "Cancel", false);
        }
    }
}
