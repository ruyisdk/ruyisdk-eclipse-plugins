package org.ruyisdk.ruyi.preferences;

import java.io.IOException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;

/**
 * Root preference page for RuyiSDK.
 */
public class RootPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private static final PluginLogger LOGGER = Activator.getLogger();

    private AutomaticCheckPreference automaticCheckPreference;
    private RuyiInstallPathPreference installPreference;

    @Override
    public void init(IWorkbench workbench) {
        // no preference store needed — we use RuyiProperties directly
    }

    @Override
    protected Control createContents(Composite parent) {
        final var content = new Composite(parent, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setLayout(new GridLayout(1, false));

        final var ruyiInstallGroup = new Group(content, SWT.NONE);
        ruyiInstallGroup.setText("Ruyi Installation");
        ruyiInstallGroup.setLayout(new GridLayout(1, false));
        ruyiInstallGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Automatic Check
        automaticCheckPreference = new AutomaticCheckPreference(ruyiInstallGroup);
        automaticCheckPreference.createSection();

        // Install Path
        installPreference = new RuyiInstallPathPreference(ruyiInstallGroup);
        installPreference.createSection();

        // Documentation Link
        final var helpLink = new Link(content, SWT.NONE);
        helpLink.setText("RuyiSDK Documentation: <a>https://ruyisdk.org/docs/intro</a>");
        {
            final var gridData = new GridData(SWT.FILL, SWT.END, true, false);
            gridData.grabExcessVerticalSpace = true;
            helpLink.setLayoutData(gridData);
        }
        helpLink.addListener(SWT.Selection, e -> {
            Program.launch("https://ruyisdk.org/docs/intro");
        });

        return content;
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        automaticCheckPreference.defaultedAutomaticDetectionState();
        installPreference.defaultedInstallPath();
    }

    @Override
    public boolean performOk() {
        try {
            automaticCheckPreference.setAutomaticDetection();
            installPreference.saveInstallPath();
        } catch (IOException e) {
            LOGGER.logError("Failed to save preferences", e);
            return false;
        }
        return true;
    }
}
