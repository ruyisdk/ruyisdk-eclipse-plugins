package org.ruyisdk.packages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;
import org.ruyisdk.packages.PackageExplorerView.OutputLiveDialog;
import org.ruyisdk.ruyi.util.RuyiFileUtils;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Control;
import org.ruyisdk.packages.JsonParser;




public class PackageExplorerView extends ViewPart {
    private CheckboxTreeViewer viewer;
    private Process bashProcess;
    private BufferedWriter bashWriter;
    private BufferedReader bashReader;
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
        buttonComposite.setLayout(new GridLayout(6, false));
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
                Runtime.getRuntime().exec(new String[] { "xdg-open", downloadDir });
            } catch (IOException e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Cannot open compressed package download directory: " + e.getMessage());
            }
        });

        // add open BinaryDirectory button
        Button openBinariesButton = new Button(buttonComposite, SWT.PUSH);
        openBinariesButton.setText("Open Binaries Dir");
        openBinariesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        openBinariesButton.addListener(SWT.Selection, event -> {
            try {
                String dataHome = System.getenv("XDG_DATA_HOME");
                String arch = System.getProperty("os.arch");
                    if ("amd64".equals(arch)) {
                        arch = "x86_64";
                    }
                    if ("i386".equals(arch)) {
                        arch = "x86_64";
                    }
                String binariesDir;
                if (dataHome != null && !dataHome.isEmpty()) {
                    binariesDir = dataHome + "/ruyi/binaries/" + arch;
                } else {
                    binariesDir = System.getProperty("user.home") + "/.local/share/ruyi/binaries/" + arch;
                    System.err.println(arch);
                }
                Runtime.getRuntime().exec(new String[] { "xdg-open", binariesDir });
            } catch (IOException e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Cannot open binary files download directory: " + e.getMessage());
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
                Runtime.getRuntime().exec(new String[] { "xdg-open", blobsDir });
            } catch (IOException e) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Cannot open image files download directory: " + e.getMessage());
            }
        });

        

        // add download button
        Button downloadButton = new Button(buttonComposite, SWT.PUSH);
        downloadButton.setText("Download");
        downloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        downloadButton.addListener(SWT.Selection, event -> {
            Object[] checkedElements = viewer.getCheckedElements();
            List<TreeNode> selectedNodes = new ArrayList<>();
            for (Object obj : checkedElements) {//Download Monitor
                if (obj instanceof TreeNode) {
                    TreeNode node = (TreeNode) obj;
                    if (node.isLeaf()&&!node.isDownloaded()) {
                        selectedNodes.add(node);
                    }
                }
            }
            if (selectedNodes.isEmpty()) {
                MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Point:", "No files selected!");
                return;
            }
            boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Confirm Download", "Are you sure you want to download the selected files?");
            if (confirmed) {
                for (TreeNode node : selectedNodes) {
                    executeInstallCommand(node.getInstallCommand());
                }
            }
        });
        //add switch board button
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

        // Start a persistent Bash session and enable experimental mode
        startBashSession();

        Display.getDefault().asyncExec(() -> {
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
    }



    private java.util.Set<String> getDownloadedFiles() {
        java.util.Set<String> files = new java.util.HashSet<>();

        // distfiles
        String cacheHome = System.getenv("XDG_CACHE_HOME");
        String distfilesDir = (cacheHome != null && !cacheHome.isEmpty())
                ? cacheHome + "/ruyi/distfiles"
                : System.getProperty("user.home") + "/.cache/ruyi/distfiles";
        addFilesFromDir(files, distfilesDir);

        // binaries
        String dataHome = System.getenv("XDG_DATA_HOME");
        String arch = System.getProperty("os.arch");
        String binariesDir = (dataHome != null && !dataHome.isEmpty())
                ? dataHome + "/ruyi/binaries/" + arch
                : System.getProperty("user.home") + "/.local/share/ruyi/binaries/" + arch;
        addFilesFromDir(files, binariesDir);

        // blobs
        String blobsDir = (dataHome != null && !dataHome.isEmpty())
                ? dataHome + "/ruyi/blobs"
                : System.getProperty("user.home") + "/.local/share/ruyi/blobs";
        addFilesFromDir(files, blobsDir);

        return files;
    }

    // Helper method: Add all file names in the directory to the set
    private void addFilesFromDir(java.util.Set<String> files, String dirPath) {
        try (java.nio.file.DirectoryStream<java.nio.file.Path> stream =
                    java.nio.file.Files.newDirectoryStream(java.nio.file.Paths.get(dirPath))) {
            for (java.nio.file.Path entry : stream) {
                files.add(entry.getFileName().toString());
            }
        } catch (Exception e) {
            // Ignore if the directory does not exist or is not accessible
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void dispose() {
        closeBashSession();
        super.dispose();
    }

    private void startBashSession() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-i");
            processBuilder.redirectErrorStream(true);
            bashProcess = processBuilder.start();

            bashWriter = new BufferedWriter(new OutputStreamWriter(bashProcess.getOutputStream()));
            bashReader = new BufferedReader(new InputStreamReader(bashProcess.getInputStream()));

            bashWriter.write("export RUYI_EXPERIMENTAL=true\n");
            bashWriter.flush();

            new Thread(() -> {
                try {
                    String line;
                    while ((line = bashReader.readLine()) != null) {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Cannot start Bash session: " + e.getMessage());
        }
    }

    private void closeBashSession() {
        try {
            if (bashWriter != null) {
                bashWriter.write("exit\n");
                bashWriter.flush();
                bashWriter.close();
            }
            if (bashReader != null) {
                bashReader.close();
            }
            if (bashProcess != null) {
                bashProcess.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeCommandInBackground(String command) {
        new Thread(() -> {
            try {
                List<String> cmdList = new ArrayList<>();
                cmdList.add("bash");
                cmdList.add("-c");
                cmdList.add(command);

                ProcessBuilder pb = new ProcessBuilder(cmdList);
                pb.environment().put("RUYI_EXPERIMENTAL", "true");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder outputBuilder = new StringBuilder();
                outputBuilder.append("[");
                boolean first = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("RUYI_DONE")) {
                        break;
                    }
                    line = line.trim();
                    // Only concatenate packet data and filter logs
                    if (!line.isEmpty() && line.startsWith("{") && !line.contains("\"ty\":\"log-v1\"")) {
                        if (!first) {
                            outputBuilder.append(",");
                        }
                        outputBuilder.append(line);
                        first = false;
                    }
                }
                outputBuilder.append("]");
                String jsonData = outputBuilder.toString();


                process.waitFor();
                reader.close();

                Display.getDefault().asyncExec(() -> {
                    try {
                        TreeNode root = JsonParser.parseJson(jsonData, getDownloadedFiles(), chosenType);
                        viewer.setInput(root);
                        viewer.expandAll();
                        markDownloadedNodes(root);
                    } catch (Exception e) {
                        MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Failed to parse JSON data: " + e.getMessage());
                    }
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Failed to execute command: " + e.getMessage());
                });
            }
        }).start();
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




    private void executeInstallCommand(String installCommand) {
        Display.getDefault().asyncExec(() -> {
            OutputLiveDialog dialog = new OutputLiveDialog(Display.getDefault().getActiveShell(), installCommand);
            dialog.open();
        });
    }

    // Real-time output dialog
    class OutputLiveDialog extends Dialog {
        private String installCommand;
        private Text text;
    
        public OutputLiveDialog(Shell parentShell, String installCommand) {
            super(parentShell);
            this.installCommand = installCommand;
        }
    
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));
            text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            text.setText("Executing command:\n" + installCommand + "\n\nOutput:\n");
            startCommand();
            return container;
        }
    


        private void startCommand() {
            new Thread(() -> {
                try {
                    List<String> cmdList = new ArrayList<>();
                    cmdList.add("bash");
                    cmdList.add("-c");
                    cmdList.add(installCommand + " && echo RUYI_DONE");
        
                    ProcessBuilder pb = new ProcessBuilder(cmdList);
                    pb.redirectErrorStream(true);
        
                    // Ensure that HOME and XDG_CACHE_HOME environment variables are consistent
                    String home = System.getProperty("user.home");
                    pb.environment().put("HOME", home);
                    String xdgCacheHome = System.getenv("XDG_CACHE_HOME");
                    if (xdgCacheHome != null && !xdgCacheHome.isEmpty()) {
                        pb.environment().put("XDG_CACHE_HOME", xdgCacheHome);
                    }
        
                    Process process = pb.start();
        
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line + "\n";
                        Display.getDefault().asyncExec(() -> {
                            if (text != null && !text.isDisposed()) {
                                text.append(outputLine);
                            }
                        });
                        if (line.contains("RUYI_DONE")) {
                            Display.getDefault().asyncExec(() -> refreshList());
                            break;
                        }
                    }
                    reader.close();
                    process.waitFor();
                } catch (IOException | InterruptedException e) {
                    Display.getDefault().asyncExec(() -> {
                        if (text != null && !text.isDisposed()) {
                            text.append("Failed to execute the installation command: " + e.getMessage() + "\n");
                        }
                    });
                }
            }).start();
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
        String ruyiPath = RuyiFileUtils.getInstallPath() + "/ruyi";
        String command = ruyiPath + " --porcelain list --related-to-entity device:" + chosenType + " ; echo RUYI_DONE";
        executeCommandInBackground(command);
    }



    private String[] fetchHardwareEntities() {
            List<String> entityIds = new ArrayList<>();
            String ruyiPath = RuyiFileUtils.getInstallPath() + "/ruyi";
            String command = "RUYI_EXPERIMENTAL=x " + ruyiPath + " --porcelain entity list -t device";
            
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
    
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder outputBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line.trim());
                }
                process.waitFor();
                // Call JsonParser to parse all entity_id
                entityIds = JsonParser.parseAllEntityIdsInOneLine(outputBuilder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entityIds.toArray(new String[0]);
    }


        private String showHardwareTypeSelectionDialog(Shell shell) {
            String[] hardwareTypes = fetchHardwareEntities();
            //add no res found check
            if (hardwareTypes == null || hardwareTypes.length == 0) {
                MessageDialog.openWarning(shell, "No Hardware Found", "Could not find any supported development board entities. Please check your Ruyi installation.");
                return null;
            }
    
            HardwareSelectionDialog dialog = new HardwareSelectionDialog(shell, hardwareTypes);
            if (dialog.open() == Dialog.OK) {
                return dialog.getSelectedHardwareType();
            }
            return null; // User cancelled
        }


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
    }
    
}