package org.ruyisdk.devices.views;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.devices.model.Device;

/**
 * Dialog for adding or editing device configurations.
 */
public class DeviceDialog extends TitleAreaDialog {
    private Device device;
    private String titleText;
    private Text nameText;
    private Text chipText;
    private Text vendorText;
    private Text versionText;

    /**
     * Constructs a new device dialog.
     *
     * @param parentShell the parent shell
     * @param device existing device to edit, or null for new device
     */
    public DeviceDialog(Shell parentShell, Device device) {
        super(parentShell);
        this.device = device;
        this.titleText = device == null ? "Add New Device" : "Edit Device";
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Device Manage"); // 这是窗口标题栏文字（不是对话框内容区的标题）
    }


    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(this.titleText);
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        new Label(container, SWT.NONE).setText("Name:");
        nameText = new Text(container, SWT.BORDER);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(container, SWT.NONE).setText("Chip:");
        chipText = new Text(container, SWT.BORDER);
        chipText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(container, SWT.NONE).setText("Vendor:");
        vendorText = new Text(container, SWT.BORDER);
        vendorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(container, SWT.NONE).setText("Version:");
        versionText = new Text(container, SWT.BORDER);
        versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        if (device != null) {
            nameText.setText(device.getName());
            chipText.setText(device.getChip());
            vendorText.setText(device.getVendor());
            versionText.setText(device.getVersion());
        }

        return area;
    }


    @Override
    protected void okPressed() {
        device = new Device(nameText.getText(), chipText.getText(), vendorText.getText(), versionText.getText(), false);
        super.okPressed();
    }

    /**
     * Gets the device configured in this dialog.
     *
     * @return the device
     */
    public Device getDevice() {
        return device;
    }
}
