package org.ruyisdk.ruyi.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class InstallProgressComposite extends Composite {
    private ProgressBar progressBar;
    private Label statusLabel;
    private Text logText;

    public InstallProgressComposite(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // 进度条
        progressBar = new ProgressBar(this, SWT.SMOOTH);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // 状态标签
        statusLabel = new Label(this, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        statusLabel.setText("Preparing installation...");

        // 日志区域
        Group logGroup = new Group(this, SWT.NONE);
        logGroup.setText("Installation Log");
        logGroup.setLayout(new GridLayout());
        logGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        logText = new Text(logGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData logData = new GridData(SWT.FILL, SWT.FILL, true, true);
        logData.heightHint = 150;
        logText.setLayoutData(logData);
    }

    public void updateProgress(int value, String message) {
        if (!isDisposed()) {
            getDisplay().asyncExec(() -> {
                if (!progressBar.isDisposed()) {
                    progressBar.setSelection(value);
                }
                if (!statusLabel.isDisposed()) {
                    statusLabel.setText(message);
                }
            });
        }
    }

    public void appendLog(String text) {
        if (!isDisposed()) {
            getDisplay().asyncExec(() -> {
                if (!logText.isDisposed()) {
                    logText.append(text + "\n");
                    logText.setTopIndex(logText.getLineCount() - 1);
                }
            });
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (!isDisposed()) {
            getDisplay().asyncExec(() -> {
                if (!progressBar.isDisposed()) {
                    progressBar.setState(indeterminate ? SWT.INDETERMINATE : SWT.NORMAL);
                }
            });
        }
    }
}
