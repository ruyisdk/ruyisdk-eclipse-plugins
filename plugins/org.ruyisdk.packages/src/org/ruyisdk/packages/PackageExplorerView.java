package org.ruyisdk.packages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
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
        // 创建按钮容器
        Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(6, false));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // 添加刷新按钮
        Button refreshButton = new Button(buttonComposite, SWT.PUSH);
        refreshButton.setText("刷新列表");
        refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        refreshButton.addListener(SWT.Selection, event -> refreshList());

        // 添加“打开下载目录”按钮
        Button openDirButton = new Button(buttonComposite, SWT.PUSH);
        openDirButton.setText("打开压缩包下载目录");
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
                MessageDialog.openError(Display.getDefault().getActiveShell(), "错误", "无法打开压缩包下载目录：" + e.getMessage());
            }
        });

                // 添加“打开二进制文件下载目录”按钮
        Button openBinariesButton = new Button(buttonComposite, SWT.PUSH);
        openBinariesButton.setText("打开二进制文件下载目录");
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
                MessageDialog.openError(Display.getDefault().getActiveShell(), "错误", "无法打开二进制文件下载目录：" + e.getMessage());
            }
        });
        
        // 添加“打开镜像文件下载目录”按钮
        Button openBlobsButton = new Button(buttonComposite, SWT.PUSH);
        openBlobsButton.setText("打开镜像文件下载目录");
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
                MessageDialog.openError(Display.getDefault().getActiveShell(), "错误", "无法打开镜像文件下载目录：" + e.getMessage());
            }
        });

        

        // 添加下载按钮
        Button downloadButton = new Button(buttonComposite, SWT.PUSH);
        downloadButton.setText("下载");
        downloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        downloadButton.addListener(SWT.Selection, event -> {
            System.out.println("下载按钮被点击");
            Object[] checkedElements = viewer.getCheckedElements();
            List<TreeNode> selectedNodes = new ArrayList<>();
            for (Object obj : checkedElements) {//下载监听
                if (obj instanceof TreeNode) {
                    TreeNode node = (TreeNode) obj;
                    if (node.isLeaf()&&!node.isDownloaded()) {
                        selectedNodes.add(node);
                    }
                }
            }
            if (selectedNodes.isEmpty()) {
                MessageDialog.openInformation(Display.getDefault().getActiveShell(), "提示", "未选中任何文件！");
                return;
            }
            boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "确认下载", "是否确认下载选中的文件？");
            if (confirmed) {
                for (TreeNode node : selectedNodes) {
                    executeInstallCommand(node.getInstallCommand());
                }
            }
        });
                // ...existing code...
        // 添加“切换开发板”按钮
        Button switchBoardButton = new Button(buttonComposite, SWT.PUSH);
        switchBoardButton.setText("选择开发板");
        switchBoardButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        switchBoardButton.addListener(SWT.Selection, event -> {
            String newType = showHardwareTypeSelectionDialog(parent.getShell());
            if (newType != null && !newType.equals(chosenType)) {
                chosenType = newType;
                refreshList();
            }
        });
        // ...existing code...

        viewer = new CheckboxTreeViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        Tree tree = viewer.getTree();
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.setContentProvider(new TreeContentProvider());
        viewer.setLabelProvider(new TreeLabelProvider());

        // 只让叶子节点有复选框
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element) {
                return false; // 默认不选中
            }
            @Override
            public boolean isGrayed(Object element) {
                // 非叶子节点灰掉（不可选）
                if (element instanceof TreeNode) {
                    return !((TreeNode) element).isLeaf();
                }
                return false;
            }
        });

        // 禁止非叶子节点被选中
        viewer.addCheckStateListener(event -> {
            Object element = event.getElement();
            if (element instanceof TreeNode && !((TreeNode) element).isLeaf()) {
                viewer.setChecked(element, false);
            }
        });
        
        // 启动持久的 Bash 会话并开启实验模式
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

    // 辅助方法：将目录下所有文件名加入集合
    private void addFilesFromDir(java.util.Set<String> files, String dirPath) {
        try (java.nio.file.DirectoryStream<java.nio.file.Path> stream =
                    java.nio.file.Files.newDirectoryStream(java.nio.file.Paths.get(dirPath))) {
            for (java.nio.file.Path entry : stream) {
                files.add(entry.getFileName().toString());
            }
        } catch (Exception e) {
            // 目录不存在或无权限时忽略
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
            System.out.println("实验模式已开启");

            new Thread(() -> {
                try {
                    String line;
                    while ((line = bashReader.readLine()) != null) {
                        System.out.println("[Bash Output] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.openError(Display.getDefault().getActiveShell(), "错误", "无法启动 Bash 会话：" + e.getMessage());
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
            System.out.println("Bash 会话已关闭");
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
                    // 只拼接包数据，过滤日志
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
                System.out.println("当前环境变量: " + pb.environment());
                System.out.println(command);
                System.out.println("接收到的 JSON 数据: " + jsonData);

                Display.getDefault().asyncExec(() -> {
                    try {
                        TreeNode root = JsonParser.parseJson(jsonData, getDownloadedFiles(), chosenType);
                        viewer.setInput(root);
                        viewer.expandAll();
                        markDownloadedNodes(root);
                    } catch (Exception e) {
                        MessageDialog.openError(Display.getDefault().getActiveShell(), "错误", "解析 JSON 数据失败：" + e.getMessage());
                    }
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), "错误", "执行命令失败：" + e.getMessage());
                });
            }
        }).start();
    }
 
    // 递归标记已下载节点
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
    
    // 新的实时输出对话框
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
            text.setText("执行命令:\n" + installCommand + "\n\n输出结果:\n");
            startCommand();
            return container;
        }
    


        private void startCommand() {
            new Thread(() -> {
                try {
                    List<String> cmdList = new ArrayList<>();
                    cmdList.add("bash");
                    cmdList.add("-c");
                    // 直接执行 installCommand，不加实验模式
                    cmdList.add(installCommand + " && echo RUYI_DONE");
        
                    ProcessBuilder pb = new ProcessBuilder(cmdList);
                    pb.redirectErrorStream(true);
        
                    // 保证 HOME 和 XDG_CACHE_HOME 环境变量一致
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
                            text.append("执行安装命令失败：" + e.getMessage() + "\n");
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
            createButton(parent, OK, "确定", true); // 只创建“确定”按钮
        }
    }
// 自定义对话框
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
        // 设置弹窗初始大小
        return new org.eclipse.swt.graphics.Point(600, 400);
    }
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, OK, "确定", true); // 只创建“确定”按钮
    }
}



    // private void refreshList() {
    //     if (chosenType == null || chosenType.isEmpty()) {
    //         System.out.println("未选择硬件类型，无法刷新列表。");
    //         return;
    //     }
    //     System.out.println("开始刷新列表...");
    //     String command = "ruyi --porcelain list --related-to-entity device:" + chosenType + " ; echo RUYI_DONE";
    //     executeCommandInBackground(command);
    // }
        private void refreshList() {
        if (chosenType == null || chosenType.isEmpty()) {
            System.out.println("未选择硬件类型，无法刷新列表。");
            return;
        }
        String ruyiPath = RuyiFileUtils.getInstallPath() + "/ruyi";
        String command = ruyiPath + " --porcelain list --related-to-entity device:" + chosenType + " ; echo RUYI_DONE";
        executeCommandInBackground(command);
    }

    private String showHardwareTypeSelectionDialog(Shell shell) {
        String[] hardwareTypes = { "sipeed-lpi4a" ,"milkv-duos"};
        ListDialog dialog = new ListDialog(shell, hardwareTypes);
        return dialog.open();
    }
}