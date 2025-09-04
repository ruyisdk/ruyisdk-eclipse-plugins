package org.ruyisdk.ruyi.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class VersionCompareComposite extends Composite {
    private StyledText currentVersionText;
    private StyledText newVersionText;

    public VersionCompareComposite(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, true));
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // 当前版本面板
        Group currentGroup = new Group(this, SWT.NONE);
        currentGroup.setText("Current Version");
        currentGroup.setLayout(new GridLayout());
        currentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        currentVersionText = new StyledText(currentGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        currentVersionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        currentVersionText.setEditable(false);

        // 新版本面板
        Group newGroup = new Group(this, SWT.NONE);
        newGroup.setText("New Version");
        newGroup.setLayout(new GridLayout());
        newGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        newVersionText = new StyledText(newGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        newVersionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        newVersionText.setEditable(false);
    }

    public void setVersions(String currentVersion, String newVersion, String changelog) {
        if (!isDisposed()) {
            getDisplay().asyncExec(() -> {
                if (!currentVersionText.isDisposed()) {
                    currentVersionText.setText("Version: " + currentVersion + "\n\nNo update information available");
                }
                if (!newVersionText.isDisposed()) {
                    newVersionText.setText("Version: " + newVersion + "\n\nChanges:\n" + changelog);
                }
            });
        }
    }

    public void highlightDifferences() {
        // 实现版本差异高亮逻辑
    }
}
