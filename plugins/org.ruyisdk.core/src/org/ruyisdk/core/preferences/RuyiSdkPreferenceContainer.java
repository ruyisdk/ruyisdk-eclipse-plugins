package org.ruyisdk.core.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * RuyiSDK 首选项根容器页（不包含具体配置项，仅作为分类入口）
 */
public class RuyiSdkPreferenceContainer extends PreferencePage implements IWorkbenchPreferencePage {

    @Override
    public void init(IWorkbench workbench) {
        // 无初始化操作
    }

    @Override
    protected Control createContents(Composite parent) {
        // 创建容器
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // 添加提示文本
        Label infoLabel = new Label(container, SWT.WRAP);
        infoLabel.setText("Expand the tree to edit preferences for a specific feature.");

        // 添加间距
        new Label(container, SWT.NONE); // 空标签作为间距

        // 可选：添加更多指引
        // Label hintLabel = new Label(container, SWT.WRAP);
        // hintLabel.setText("Note: Configuration changes may require restart to take effect.");
        Link helpLink = new Link(container, SWT.NONE);
        helpLink.setText("RuyiSDK 文档: <a>Click here for online documentation</a>");
        helpLink.addListener(SWT.Selection, e -> {
            Program.launch("https://ruyisdk.org/docs/intro");
        });

        return container;
    }

    @Override
    public boolean performOk() {
        // 无需保存操作
        return true;
    }

    @Override
    protected void performDefaults() {
        // 无默认值操作
    }
}
