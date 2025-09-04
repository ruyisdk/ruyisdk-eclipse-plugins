package org.ruyisdk.ruyi.preferences;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.ruyi.services.RuyiProperties;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

public class RuyiInstallPathPreference {
    private final Composite parent;
    private Text ruyiInstallPathText;

    public RuyiInstallPathPreference(Composite parent) {
        this.parent = parent;
    }

    public void createSection() {
        Composite container = new Composite(parent, SWT.NONE);
        // container.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.verticalIndent = 0; // 明确取消缩进
        container.setLayoutData(gd);
        container.setLayout(new GridLayout(3, false)); // 标签+文本框+按钮

        // 创建标签
        Label label = new Label(container, SWT.NONE);
        label.setText("Ruyi Installation Directory:");
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        // 创建文本框
        ruyiInstallPathText = new Text(container, SWT.BORDER);
        ruyiInstallPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ruyiInstallPathText.setText(RuyiFileUtils.getInstallPath());

        // 创建浏览按钮
        Button browse = new Button(container, SWT.PUSH);
        browse.setText("Browse...");
        browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        browse.addListener(SWT.Selection, e -> {
            DirectoryDialog dialog = new DirectoryDialog(Display.getDefault().getActiveShell());
            dialog.setFilterPath(ruyiInstallPathText.getText());
            String path = dialog.open();
            if (path != null)
                ruyiInstallPathText.setText(path);
        });


    }


    public void defaultedInstallPath() {
        ruyiInstallPathText.setText(RuyiFileUtils.getDefaultInstallPath().toString());
    }

    public void saveInstallPath() throws IOException {
        String newPath = ruyiInstallPathText.getText();
        RuyiProperties.setInstallPath(newPath);
    }

    public String getTextPath() {
        return ruyiInstallPathText.getText().trim();
    }

    // 持久化逻辑
    // public String saveRuyiPath() {
    // getPreferenceStore().setDefault("RUYI_INSTALL_PATH", getInstallPath());
    // ruyiInstallPathText.addModifyListener(e -> {
    // getPreferenceStore().setValue("RUYI_INSTALL_PATH", ruyiInstallPathText.getText());
    // });
    // }
    // 自定义恢复逻辑
    // getPreferenceStore().setToDefault("RUYI_INSTALL_PATH");
    // getPreferenceStore().setToDefault("ENABLE_TELEMETRY");

}
