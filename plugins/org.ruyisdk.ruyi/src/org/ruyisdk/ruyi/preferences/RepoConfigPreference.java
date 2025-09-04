package org.ruyisdk.ruyi.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.ruyisdk.core.config.Constants;
import org.ruyisdk.core.ruyi.model.RepoConfig;
import org.ruyisdk.ruyi.services.RuyiProperties;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

public class RepoConfigPreference {
    private final Composite parent;
    private Button[] repoCheckboxes;
    private Text customRepoText;
    private RepoConfig[] configs;

    public RepoConfigPreference(Composite parent) {
        // this.parent = new Composite(parent, SWT.NONE);
        // this.parent.setLayout(new GridLayout(1, false));
        //
        // GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        // this.parent.setLayoutData(gridData); // 添加这行确保布局数据

        this.parent = parent;
    }

    public void createSection() {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Package Repositories");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        // group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); // 修改布局数据

        configs = getRepoConfigs();
        repoCheckboxes = new Button[configs.length];

        for (int i = 0; i < configs.length; i++) {
            if ("Custom".equals(configs[i].getName())) {
                createCustomRepoUI(group, configs[i], i);
            } else {
                createStandardRepoUI(group, configs[i], i);
            }
        }

        // 添加调试输出
        // System.out.println("Repo section created with " + configs.length + " items");
    }

    private void createCustomRepoUI(Composite parent, RepoConfig config, int index) {
        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(new GridLayout(2, false));
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        repoCheckboxes[index] = new Button(row, SWT.CHECK);
        repoCheckboxes[index].setText(config.getName());
        repoCheckboxes[index].setSelection(config.getCheckState());
        repoCheckboxes[index].addListener(SWT.Selection, e -> {
            customRepoText.setEnabled(repoCheckboxes[index].getSelection());
        });

        String customUrl = RuyiProperties.getCustomMirror();
        customRepoText = new Text(row, SWT.BORDER);
        customRepoText.setText(customUrl);
        customRepoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        customRepoText.setEnabled(customUrl.isEmpty() ? false : true);
    }

    private void createStandardRepoUI(Composite parent, RepoConfig config, int index) {
        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(new GridLayout(1, false));
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        repoCheckboxes[index] = new Button(parent, SWT.CHECK);
        repoCheckboxes[index].setText(config.getName() + ": " + config.getUrl());
        repoCheckboxes[index].setSelection(config.getCheckState());

        // 确保有布局数据
        repoCheckboxes[index].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    public RepoConfig[] getRepoDefault() {
        // return new RepoConfig[] {
        // new RepoConfig("China ISCAS Mirror", "https://mirror.iscas.ac.cn/git/ruyisdk/packages-index.git",
        // 1 , true),
        // new RepoConfig("GitHub Repo", "https://github.com/ruyisdk/packages-index.git", 2, true),
        // new RepoConfig("Custom", "", 0, false) };
        return new RepoConfig[] {new RepoConfig("China ISCAS Mirror", Constants.NetAddress.BACKUP_REPO_URL, 1, true),
                        new RepoConfig("GitHub Repo", Constants.NetAddress.MAIN_REPO_URL, 2, true),
                        new RepoConfig("Custom", "", 0, false)};
    }

    public RepoConfig[] getRepoConfigs() {
        return new RepoConfig[] {
                        new RepoConfig("China ISCAS Mirror", Constants.NetAddress.BACKUP_REPO_URL, 1,
                                        RuyiProperties.isIscasMirrorChecked()),
                        new RepoConfig("GitHub Repo", Constants.NetAddress.MAIN_REPO_URL, 2,
                                        RuyiProperties.isGithubMirrorChecked()),
                        new RepoConfig("Custom", RuyiProperties.getCustomMirror(), 0,
                                        RuyiProperties.isCustomMirrorChecked())};
    }

    public RepoConfig[] getSelectedRepos() {
        List<RepoConfig> selectedConfigs = new ArrayList<>();

        for (int i = 0; i < repoCheckboxes.length; i++) {
            if (repoCheckboxes[i].getSelection()) {
                // 如果是自定义仓库，更新URL
                if ("Custom".equals(configs[i].getName())) {
                    configs[i].setUrl(customRepoText.getText().trim());
                }
                // 只有当URL不为空时才添加到选中列表
                if (!configs[i].getUrl().isEmpty()) {
                    selectedConfigs.add(configs[i]);
                }
            }
        }
        // Sort by priority (ascending order)
        selectedConfigs.sort(Comparator.comparingInt(RepoConfig::getPriority));

        return selectedConfigs.toArray(new RepoConfig[0]);
    }

    public void defaultedRepoState() {
        RepoConfig[] defaultConfigs = getRepoDefault();
        for (int i = 0; i < defaultConfigs.length; i++) {
            if ("Custom".equals(defaultConfigs[i].getName())) {
                // boolean state = defaultConfigs[i].getCheckState();
                customRepoText.setText("");
                customRepoText.setEnabled(false);
                repoCheckboxes[i].setSelection(false);
            } else {
                repoCheckboxes[i].setSelection(defaultConfigs[i].getCheckState());
            }
        }
    }

    public void saveRepoConfigs() throws IOException {
        for (int i = 0; i < repoCheckboxes.length; i++) {
            if (repoCheckboxes[i].getText().contains("ISCAS")) {
                RuyiProperties.setIscasMirrorChecked(repoCheckboxes[i].getSelection());
            } else if (repoCheckboxes[i].getText().contains("GitHub")) {
                RuyiProperties.setGithubMirrorChecked(repoCheckboxes[i].getSelection());
            } else if (repoCheckboxes[i].getText().contains("Custom")) {
                boolean f = repoCheckboxes[i].getSelection();
                RuyiProperties.setCustomMirrorChecked(f);

                String text = customRepoText.getText();
                // if (f && text != null && !"".equals(text)) {
                RuyiProperties.setCustomMirror(text.trim());
                // }
            } else {
                System.out.println("repoCheckboxes text cannot be matched :" + repoCheckboxes[i].getText());
            }
        }
    }
}
