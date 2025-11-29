package org.ruyisdk.ruyi.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.ruyisdk.core.ruyi.model.RepoConfig;
import org.ruyisdk.ruyi.services.RuyiProperties;
import org.ruyisdk.ruyi.services.RuyiProperties.TelemetryStatus;

/**
 * Preference component for telemetry settings.
 */
public class TelemetryPreference {
    private final Composite parent;
    // private Button telemetryCheckbox;
    private Combo telemetryCombo;

    /**
     * Constructs preference component.
     *
     * @param parent parent composite
     */
    public TelemetryPreference(Composite parent) {
        this.parent = parent;
        // this.parent = new Composite(parent, SWT.NONE);
        // this.parent.setLayout(new GridLayout(1, false));
        // this.parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    /**
     * Creates UI section.
     */
    public void createSection() {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Telemetry Settings");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        // group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // checkbox
        // telemetryCheckbox = new Button(group, SWT.CHECK);
        // telemetryCheckbox.setText("Enable anonymous usage data collection");
        // telemetryCheckbox.setSelection(true);
        // telemetryCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        // Create a container for label + combo in one row
        Composite comboContainer = new Composite(group, SWT.NONE);
        comboContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        comboContainer.setLayout(new GridLayout(2, false));

        // Label for the combo
        Label label = new Label(comboContainer, SWT.NONE);
        label.setText("Telemetry data collection:");
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Combo for telemetry status
        telemetryCombo = new Combo(comboContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
        telemetryCombo.setItems(new String[] {"Enabled: Send anonymous usage data",
                "Local only: Analyze data locally only", "Disabled: No data collection"});
        // Set default selection
        telemetryCombo.select(initTelemetryStatus());
        telemetryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // 创建一个Link控件来显示描述文本和链接
        Link link = new Link(group, SWT.WRAP);
        String description = "RuyiSDK 遥测数据秉持最小化收集信息的原则，" + "尽可能避免收集用户个人身份信息，且采用匿名化方式收集信息"
                        + "用于产品的运营和服务的提升。可前往 Windows > Preferences > Ruyi 中进行设置修改。详见";
        String linkText = "<a href=\"https://ruyisdk.org/docs/legal/privacyPolicy\">RuyiSDK隐私政策</a>";
        link.setText(description + " " + linkText);
        // 设置Link控件的布局数据，控制宽度
        GridData linkLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        linkLayoutData.widthHint = 400; // 设置固定宽度，根据需要调整
        link.setLayoutData(linkLayoutData);

        group.layout(true);

        // 添加链接点击事件
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // 在默认浏览器中打开链接
                Program.launch(event.text);
            }
        });

    }

    // public boolean isTelemetryEnabled() {
    // return telemetryCheckbox.getSelection();
    // }
    //
    // public void defaultedTelemetryState() {
    // telemetryCheckbox.setSelection(true);
    // }
    //
    // public void saveTelemetryConfigs() throws IOException {
    // RuyiProperties.setTelemetryStatus(telemetryCheckbox.getSelection());
    // }
    /**
     * Gets telemetry status.
     *
     * @return telemetry status
     */
    public TelemetryStatus getTelemetryStatus() {
        switch (telemetryCombo.getSelectionIndex()) {
            case 0:
                return TelemetryStatus.ON;
            case 1:
                return TelemetryStatus.LOCAL;
            case 2:
                return TelemetryStatus.OFF;
            default:
                return TelemetryStatus.ON;
        }
    }

    /**
     * Sets telemetry status.
     *
     * @param status telemetry status
     */
    public void setTelemetryStatus(TelemetryStatus status) {
        switch (status) {
            case ON:
                telemetryCombo.select(0);
                break;
            case LOCAL:
                telemetryCombo.select(1);
                break;
            case OFF:
                telemetryCombo.select(2);
                break;
            default:
                telemetryCombo.select(0);
                break;
        }
    }

    /**
     * Sets default telemetry state.
     */
    public void defaultedTelemetryState() {
        telemetryCombo.select(0); // Default to ON
    }

    /**
     * Saves telemetry configurations.
     *
     * @throws IOException if save fails
     */
    public void saveTelemetryConfigs() throws IOException {
        RuyiProperties.setTelemetryStatus(getTelemetryStatus());
    }

    /**
     * Initializes telemetry status.
     *
     * @return status index
     */
    public int initTelemetryStatus() {
        switch (RuyiProperties.getTelemetryStatus()) {
            case ON:
                return 0;
            case LOCAL:
                return 1;
            case OFF:
                return 2;
            default:
                return 0;
        }
    }

}
