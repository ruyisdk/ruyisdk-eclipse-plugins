package org.ruyisdk.ruyi.preferences;

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
import org.ruyisdk.ruyi.model.TelemetryMode;
import org.ruyisdk.ruyi.services.RuyiCli;

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

        // Combo for telemetry mode
        int telemetryIndex;
        try {
            telemetryIndex = getTelemetrySelectionIndex();
        } catch (PluginException e) {
            telemetryIndex = 0; // default to ON
        }
        telemetryCombo = new Combo(comboContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
        telemetryCombo.setItems(new String[] {"Enabled: Send anonymous usage data",
                "Local only: Analyze data locally only", "Disabled: No data collection"});
        // Set default selection
        telemetryCombo.select(telemetryIndex);
        telemetryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // 创建一个Link控件来显示描述文本和链接
        Link link = new Link(group, SWT.WRAP);
        String description = """
            RuyiSDK 遥测数据秉持最小化收集信息的原则，\
            尽可能避免收集用户个人身份信息，且采用匿名化方式收集信息\
            用于产品的运营和服务的提升。可前往 Windows > Preferences > Ruyi 中进行设置修改。详见""";
        String linkText =
                "<a href=\"https://ruyisdk.org/docs/legal/privacyPolicy\">RuyiSDK隐私政策</a>";
        link.setText(String.format("%s %s", description, linkText));
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

    /**
     * Gets telemetry mode.
     *
     * @return telemetry mode
     */
    public TelemetryMode getTelemetryMode() {
        switch (telemetryCombo.getSelectionIndex()) {
            case 0:
                return TelemetryMode.ON;
            case 1:
                return TelemetryMode.LOCAL;
            case 2:
                return TelemetryMode.OFF;
            default:
                return TelemetryMode.ON;
        }
    }

    /**
     * Sets default telemetry state.
     */
    public void defaultedTelemetryState() {
        telemetryCombo.select(0); // Default to ON
    }

    /**
     * Get telemetry mode from the CLI for combo selection.
     *
     * @return combo index (0=ON, 1=LOCAL, 2=OFF)
     */
    private int getTelemetrySelectionIndex() {
        final var raw = RuyiCli.getTelemetryMode();
        if (raw != null) {
            final var lower = raw.trim().toLowerCase();
            if (lower.equals("local")) {
                return 1;
            } else if (lower.equals("off")) {
                return 2;
            }
        }
        // default to ON
        return 0;
    }

}
