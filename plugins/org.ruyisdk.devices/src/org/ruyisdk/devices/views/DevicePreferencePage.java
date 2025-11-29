package org.ruyisdk.devices.views;

import java.util.List;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.ruyisdk.devices.Activator;
import org.ruyisdk.devices.model.Device;
import org.ruyisdk.devices.providers.DeviceLabelProvider;
import org.ruyisdk.devices.services.DeviceService;

/**
 * Preference page for managing RISC-V development devices.
 */
public class DevicePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private TableViewer tableViewer;
    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private Button setDefaultButton;

    private Label infoLabel;
    private DeviceService deviceService = new DeviceService();
    private List<Device> devices;

    /**
     * Constructs the device preference page.
     */
    public DevicePreferencePage() {
        if (Activator.getDefault() == null) {
            throw new IllegalStateException("Plugin not activated!");
        }
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
        // 禁用默认按钮
        // noDefaultAndApplyButton();
        noDefaultButton();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);

        // 设置父容器的布局为 2 列
        container.setLayout(new GridLayout(2, false));

        // 提示信息
        infoLabel = new Label(container, SWT.NONE);
        infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        // 左侧区域：表格
        Composite tableComposite = new Composite(container, SWT.NONE);
        tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tableComposite.setLayout(new GridLayout(1, false));

        // 创建表格
        tableViewer = new TableViewer(tableComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = tableViewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        // 添加表头
        // String[] columnNames = { "开发板型号", "SOC", "厂商", "版本", "状态" };
        String[] columnNames = {"Name", "SOC", "Vendor", "Version", "Default"};
        for (String columnName : columnNames) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(columnName);
            column.setWidth(120);
        }

        // 设置内容提供器和标签提供器
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setLabelProvider(new DeviceLabelProvider());

        // 右侧区域：按钮（纵向排列）
        Composite buttonComposite = new Composite(container, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonComposite.setLayout(new GridLayout(1, false));

        // 添加按钮
        addButton = createButton(buttonComposite, "Add");
        editButton = createButton(buttonComposite, "Edit");
        removeButton = createButton(buttonComposite, "Remove");
        setDefaultButton = createButton(buttonComposite, "Set Default");

        // 加载数据
        refreshView();

        // 绑定按钮事件
        bindButtonEvents();

        return container;
    }

    private Button createButton(Composite parent, String text) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return button;
    }

    private void refreshView() {
        devices = deviceService.getDevices();
        tableViewer.setInput(devices);

        boolean hasBoards = !devices.isEmpty();
        final boolean hasDefault = devices.stream().anyMatch(Device::isDefault);

        editButton.setEnabled(hasBoards);
        removeButton.setEnabled(hasBoards);
        setDefaultButton.setEnabled(hasBoards);

        if (devices.isEmpty()) {
            infoLabel.setText("您还未添加任何RISC-V开发板。");
        } else if (!hasDefault) {
            infoLabel.setText("请设置默认开发板以获取IDE资源推荐。");
        } else {
            infoLabel.setText("当前管理的开发板列表：");
        }
    }

    /**
     * 刷新表格数据.
     */
    private void refreshTable() {
        tableViewer.setInput(deviceService.getDevices());
        for (TableColumn column : tableViewer.getTable().getColumns()) {
            column.pack();
        }
    }

    /**
     * 绑定按钮事件.
     */
    private void bindButtonEvents() {
        addButton.addListener(SWT.Selection, e -> handleAddDevice());
        editButton.addListener(SWT.Selection, e -> handleEditDevice());
        removeButton.addListener(SWT.Selection, e -> handleRemoveDevice());
        setDefaultButton.addListener(SWT.Selection, e -> handleSetDefault());
    }

    private void handleAddDevice() {
        DeviceDialog dialog = new DeviceDialog(getShell(), null);
        if (dialog.open() == Window.OK) {
            deviceService.addDevice(dialog.getDevice());
            refreshView();
        }
    }

    private void handleEditDevice() {
        Device selectedDevice = getSelectedDevice();
        if (selectedDevice != null) {
            DeviceDialog dialog = new DeviceDialog(getShell(), selectedDevice);
            if (dialog.open() == Window.OK) {
                deviceService.updateDevice(selectedDevice, dialog.getDevice());
                refreshView();
            }
        }
    }

    private void handleRemoveDevice() {
        Device selectedDevice = getSelectedDevice();
        if (selectedDevice != null) {
            deviceService.deleteDevice(selectedDevice);
            refreshTable();
            refreshView();
        }
    }

    private void handleSetDefault() {
        Device selectedDevice = getSelectedDevice();
        if (selectedDevice != null) {
            deviceService.setDefaultDevice(selectedDevice);
            refreshTable();
            refreshView();
        }
    }

    /**
     * 获取选中的开发板.
     */
    private Device getSelectedDevice() {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        return (Device) selection.getFirstElement();
    }

    @Override
    public boolean performOk() {
        if (!super.performOk()) {
            return false;
        }

        // 自定义保存逻辑
        try {
            deviceService.saveDevices();
            return true;
        } catch (Exception e) {
            setErrorMessage("保存失败: " + e.getMessage());
            return false;
        }
    }
}
