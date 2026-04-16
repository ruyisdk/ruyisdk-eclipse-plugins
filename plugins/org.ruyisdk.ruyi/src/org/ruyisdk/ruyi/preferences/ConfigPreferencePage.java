package org.ruyisdk.ruyi.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.services.RuyiCli;
import org.ruyisdk.ruyi.services.RuyiCliException;

/**
 * Preference page for Ruyi's configuration.
 */
public class ConfigPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private static final PluginLogger LOGGER = Activator.getLogger();
    private RepoConfigPreference repoPreference;
    private Text localPathText;
    private Button prereleasesCheckbox;
    private TelemetryPreference telemetryPreference;

    @Override
    public void init(IWorkbench workbench) {
        // no preference store needed — we use RuyiProperties directly
    }

    @Override
    protected Control createContents(Composite parent) {
        final var content = new Composite(parent, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setLayout(new GridLayout(1, false));

        // Remote Repository

        repoPreference = new RepoConfigPreference(content);
        repoPreference.createSection();

        // Local Repository

        final var localGroup = new Group(content, SWT.NONE);
        localGroup.setText("Local Repository");
        localGroup.setLayout(new GridLayout(3, false));
        localGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        final var localLabel = new Label(localGroup, SWT.NONE);
        localLabel.setText("Checkout path:");

        final var localValue = RuyiCli.getRepoLocal();
        localPathText = new Text(localGroup, SWT.BORDER);
        localPathText.setText(localValue != null && !localValue.isBlank() ? localValue.trim() : "");
        localPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final var browseButton = new Button(localGroup, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addListener(SWT.Selection, e -> {
            DirectoryDialog dirDialog = new DirectoryDialog(getShell());
            String current = localPathText.getText().trim();
            if (!current.isEmpty()) {
                dirDialog.setFilterPath(current);
            }
            String path = dirDialog.open();
            if (path != null) {
                localPathText.setText(path);
            }
        });

        final var localHint = new Label(localGroup, SWT.WRAP);
        localHint.setText("""
            Use an existing git clone of the packages-index repository \
            instead of the default cache location. Must be a valid git repository \
            or a non-existing path (ruyi will clone into it).
            Leave empty to use the location managed by ruyi.""");
        {
            final var gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.horizontalSpan = 3;
            gridData.widthHint = 400;
            localHint.setLayoutData(gridData);
        }

        // Package Options

        final var packagesGroup = new Group(content, SWT.NONE);
        packagesGroup.setText("Package Options");
        packagesGroup.setLayout(new GridLayout(1, false));
        packagesGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        prereleasesCheckbox = new Button(packagesGroup, SWT.CHECK);
        prereleasesCheckbox.setText("Include pre-release packages");
        prereleasesCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        prereleasesCheckbox.setSelection(RuyiCli.getPackagesPrereleases());

        // Telemetry Settings

        telemetryPreference = new TelemetryPreference(content);
        telemetryPreference.createSection();

        return content;
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        repoPreference.defaultedRepoState();
        localPathText.setText("");
        prereleasesCheckbox.setSelection(false);
        telemetryPreference.defaultedTelemetryState();
    }

    @Override
    public boolean performOk() {
        try {
            RuyiCli.setRepoRemote(repoPreference.getSelectedUrl());
            RuyiCli.setRepoBranch(repoPreference.getBranch());
            RuyiCli.setRepoLocal(localPathText.getText().trim());
            RuyiCli.setPackagesPrereleases(prereleasesCheckbox.getSelection());
            RuyiCli.setTelemetry(telemetryPreference.getTelemetryMode());
        } catch (RuyiCliException e) {
            LOGGER.logError("Failed to apply Ruyi config", e);
        }
        return true;
    }
}
