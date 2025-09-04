package org.ruyisdk.ruyi.ui;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.ruyisdk.core.ruyi.model.RepoConfig;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.preferences.AutomaticCheckPreference;
import org.ruyisdk.ruyi.preferences.RepoConfigPreference;
import org.ruyisdk.ruyi.preferences.RuyiInstallPathPreference;
import org.ruyisdk.ruyi.preferences.TelemetryPreference;
import org.ruyisdk.ruyi.services.RuyiInstallManager;
import org.ruyisdk.ruyi.services.RuyiProperties;
import org.ruyisdk.ruyi.services.RuyiProperties.TelemetryStatus;
import org.ruyisdk.ruyi.util.RuyiLogger;
import org.ruyisdk.ruyi.util.StatusUtil;

// Ruyi安装/升级向导
public class RuyiInstallWizard extends Wizard {
    public enum Mode {
        INSTALL, UPGRADE
    } // 向导运行模式:INSTALL全新安装模式,UPGRADE升级现有安装模式

    private final Mode mode;
    private final RuyiVersion currentVersion;
    private final RuyiVersion newVersion;

    public static void openForInstall() {
        new RuyiInstallWizard(Mode.INSTALL, null, null).open();
    }

    public static void openForUpgrade(RuyiVersion current, RuyiVersion latest) {
        new RuyiInstallWizard(Mode.UPGRADE, current, latest).open();
    }

    // 私有构造方法
    private RuyiInstallWizard(Mode mode, RuyiVersion currentVersion, RuyiVersion newVersion) {
        this.mode = mode;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        setNeedsProgressMonitor(true);

        // 根据模式设置不同标题
        setWindowTitle(mode == Mode.INSTALL ? "Ruyi Installation Wizard" : "Ruyi Upgrade Wizard");
    }

    @Override
    public void addPages() {
        addPage(new CheckResultPage(mode));
        addPage(new PreparePage(mode));
        addPage(new ConfigurationPage());
        addPage(new InstallationPage(mode));
        addPage(new CompletionPage(mode));
    }

    @Override
    public boolean performFinish() {
        return ((InstallationPage) getPage("installationPage")).performFinish();
    }

    public void open() {
        WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), this);
        dialog.setMinimumPageSize(600, 400);
        dialog.open();
    }

    // ========================各向导页面的内部类实现====================//
    private class CheckResultPage extends WizardPage {
        private final Mode mode;
        private Button dontCheckAgainCheckbox;
        // private AutomaticCheckPreference automaticCheckPref;

        public CheckResultPage(Mode mode) {
            super("checkResultPage");
            this.mode = mode;
            setTitle(mode == Mode.INSTALL ? "Ruyi Installation Required" : "Ruyi Upgrade Available");
            // setDescription("Review the detection results before proceeding");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));

            // 信息展示区域
            Label infoLabel = new Label(container, SWT.WRAP);
            infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            String showtext = mode == Mode.INSTALL ? "Ruyi package manager (ruyi command) not detected.  \n"
                            : String.format("New Ruyi version %s available (current: %s).  \n", newVersion,
                                            currentVersion);
            infoLabel.setText(showtext);


            FontData[] fontData = infoLabel.getFont().getFontData(); // 获取当前字体数据
            // 修改字体数据，设置字体名称和大小
            for (FontData fd : fontData) {
                fd.setName("Arial"); // 设置字体名称
                fd.setHeight(12); // 设置字体大小
                fd.setStyle(SWT.BOLD); // 设置字体样式为粗体
            }
            // 创建新的字体并应用到标签
            Font newFont = new Font(infoLabel.getDisplay(), fontData);
            infoLabel.setFont(newFont);

            // 注意：确保在适当的时候释放字体资源
            infoLabel.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    newFont.dispose();
                }
            });

            // 提示文本
            Label hintLabel = new Label(container, SWT.WRAP);
            hintLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            hintLabel.setText("Please note:\n" + "• Click [Next] to start installation wizard. \n" + // [Proceed Now]
                            "• Select [Cancel] to manually trigger from menu: RuyiSDK > Ruyi Installation. \n" + // [Handle
                                                                                                                 // Later]
                            "• If you already have ruyi, configure path in: Windows > Preferences > RuyiSDK > Ruyi Config > Ruyi Installation Directory. \n\n ");
            Color tipColor = new Color(hintLabel.getDisplay(), 0, 0, 255);
            hintLabel.setForeground(tipColor); // 设置标签的字体颜色
            // 颜色对象使用后需要释放资源
            hintLabel.addDisposeListener(e -> {
                if (!tipColor.isDisposed()) {
                    tipColor.dispose();
                }
            });

            // 不再检测选项
            // automaticCheckPref = new AutomaticCheckPreference(container);
            // automaticCheckPref.createSection();
            dontCheckAgainCheckbox = new Button(container, SWT.CHECK);
            dontCheckAgainCheckbox.setText("Don't check and prompt again");
            dontCheckAgainCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            dontCheckAgainCheckbox.addListener(SWT.Selection, e -> setAutomaticDetection());

            setControl(container);
        }

        // 保存"不再提示"设置
        private void setAutomaticDetection() {
            try {
                RuyiProperties.setAutomaticDetection(!dontCheckAgainCheckbox.getSelection());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class PreparePage extends WizardPage {

        public PreparePage(Mode mode) {
            super("welcomePage");
            setTitle(mode == Mode.INSTALL ? "Welcome to Ruyi Installation" : "Welcome to Ruyi Upgrade");
            setDescription(mode == Mode.INSTALL ? "This wizard will guide you through the Ruyi installation process"
                            : "This wizard will upgrade your Ruyi installation to the latest version");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));

            Label label = new Label(container, SWT.WRAP);
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            String text = mode == Mode.INSTALL
                            ? "The Ruyi SDK provides all necessary tools for Ruyi development. \n\n"
                                            + "Before proceeding, please ensure: \n" + "• 500MB+ free disk space \n"
                                            + "• Active internet connection \n" + "• Administrator privileges if needed"
                            : "Your Ruyi installation will be upgraded to the latest version. \n\n" + "Please note: \n"
                                            + "• The old version will be replaced \n"
                                            + "• Existing configurations will be preserved \n"
                                            + "• Active internet connection \n"
                                            + "• Administrator privileges if needed";

            label.setText(text);
            setControl(container);
        }
    }

    private class ConfigurationPage extends WizardPage {
        private RuyiInstallPathPreference installPref;
        private RepoConfigPreference repoPref;
        private TelemetryPreference telemetryPref;

        public ConfigurationPage() {
            super("configurationPage");
            setTitle("Configuration");
            setDescription("Configure installation options");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));

            // 安装路径配置
            installPref = new RuyiInstallPathPreference(container);
            installPref.createSection();

            // 仓库配置
            repoPref = new RepoConfigPreference(container);
            repoPref.createSection();

            // 遥测配置
            telemetryPref = new TelemetryPreference(container);
            telemetryPref.createSection();

            setControl(container);
        }

        public String getInstallPath() {
            return installPref.getTextPath();
        }

        public RepoConfig[] getSelectedRepos() {
            return repoPref.getSelectedRepos();
        }

        public TelemetryStatus getTelemetryStatus() {
            return telemetryPref.getTelemetryStatus();
        }

        public void saveConfig() {
            try {
                installPref.saveInstallPath();
                repoPref.saveRepoConfigs();
                telemetryPref.saveTelemetryConfigs();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class InstallationPage extends WizardPage {
        private final Mode mode;
        private InstallProgressComposite progressComp;
        private RuyiInstallManager installManager;

        public InstallationPage(Mode mode) {
            super("installationPage");
            this.mode = mode;
            // this.installManager = Activator.getDefault().getRuyiCore().getInstallManager();
            this.installManager = new RuyiInstallManager(Activator.getDefault().getLogger());
            setTitle(mode == Mode.INSTALL ? "Installing Ruyi" : "Upgrading Ruyi");
            setDescription(mode == Mode.INSTALL ? "Please wait while Ruyi is being installed"
                            : "Upgrading to the latest version...");
        }

        @Override
        public void createControl(Composite parent) {
            progressComp = new InstallProgressComposite(parent);
            setControl(progressComp);
        }

        @Override
        public IWizardPage getPreviousPage() {
            return null;
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                startInstallation();
            }
        }

        private void startInstallation() {
            ConfigurationPage configPage = (ConfigurationPage) getWizard().getPage("configurationPage");
            configPage.saveConfig();

            // installManager.setInstalledVersion(currentVersion.toString());
            // installManager.setLatestVersion(newVersion.toString());

            installManager.setInstallPath(configPage.getInstallPath());
            installManager.setRepoUrls(configPage.getSelectedRepos());
            installManager.setTelemetryStatus(configPage.getTelemetryStatus());

            progressComp.appendLog("Starting " + (mode == Mode.INSTALL ? "installation" : "upgrade") + "...");

            Job.create("Ruyi " + mode.name(), monitor -> {
                try {
                    installManager.install(monitor, new InstallationListener() {
                        @Override
                        public void progressChanged(int percent, String message) {
                            updateProgress(percent, message);
                        }

                        @Override
                        public void logMessage(String message) {
                            appendLog(message);
                        }
                    });

                    Display.getDefault().asyncExec(() -> {
                        progressComp.appendLog("Operation completed successfully!");
                        setPageComplete(true);
                    });

                    return Status.OK_STATUS;
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        progressComp.appendLog("Failed: " + e.getMessage());
                        setPageComplete(false);
                        StatusUtil.logAndShow(mode + " failed", e);
                    });
                    return StatusUtil.createErrorStatus(mode + " failed", e);
                }
            }).schedule();
        }

        private void updateProgress(int percent, String message) {
            Display.getDefault().asyncExec(() -> {
                progressComp.updateProgress(percent, message);
            });
        }

        private void appendLog(String message) {
            Display.getDefault().asyncExec(() -> {
                progressComp.appendLog(message);
            });
        }

        public boolean performFinish() {
            return isPageComplete();
        }
    }

    private class CompletionPage extends WizardPage {
        private final Mode mode;

        public CompletionPage(Mode mode) {
            super("completionPage");
            this.mode = mode;
            setTitle("Operation Complete");
            setDescription(mode == Mode.INSTALL ? "Ruyi has been successfully installed"
                            : "Ruyi has been upgraded successfully");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(1, false));

            Label label = new Label(container, SWT.WRAP);
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            String text = mode == Mode.INSTALL ? "Ruyi has been successfully installed!\n\n" + "Next steps:\n"
                            + "• Restart your IDE to apply changes\n" + "• Configure project SDK settings\n"
                            + "• Visit documentation for tutorials"
                            : "Ruyi has been upgraded to the latest version.\n\n" + "What's new:\n"
                                            + "• Improved performance\n" + "• New API features\n" + "• Bug fixes";

            label.setText(text);
            setControl(container);
        }

        @Override
        public boolean isPageComplete() {
            return true; // 完成页始终可进入
        }

        @Override
        public IWizardPage getPreviousPage() {
            return null;
        }
    }

    public interface InstallationListener {
        void progressChanged(int percent, String message);

        void logMessage(String message);
    }
}
