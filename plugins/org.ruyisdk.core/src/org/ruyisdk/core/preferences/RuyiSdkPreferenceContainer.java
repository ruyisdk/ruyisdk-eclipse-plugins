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
 * Root container preference page for RuyiSDK configuration.
 *
 * <p>This serves as a category entry point in the preferences dialog tree,
 * containing no actual configuration items itself. Specific preferences
 * should be implemented in sub-pages.
 */
public class RuyiSdkPreferenceContainer extends PreferencePage implements IWorkbenchPreferencePage {

    @Override
    public void init(IWorkbench workbench) {
        // No initialization required
    }

    @Override
    protected Control createContents(Composite parent) {
        // Create main container
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // Add informational label
        Label infoLabel = new Label(container, SWT.WRAP);
        infoLabel.setText("Expand the tree to edit preferences for a specific feature.");

        // Add spacing
        new Label(container, SWT.NONE); // Empty label for spacing

        // Optional: Add documentation link
        Link helpLink = new Link(container, SWT.NONE);
        helpLink.setText("RuyiSDK Documentation: <a>Click here for online documentation</a>");
        helpLink.addListener(SWT.Selection, e -> {
            Program.launch("https://ruyisdk.org/docs/intro");
        });

        return container;
    }

    @Override
    public boolean performOk() {
        // No save operation required
        return true;
    }

    @Override
    protected void performDefaults() {
        // No default values to restore
    }
}