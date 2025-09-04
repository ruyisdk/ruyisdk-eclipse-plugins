package org.ruyisdk.projectcreator.wizards;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.projectcreator.utils.ToolchainLocator;

public class ProjectSettingsPage extends WizardPage {
    // UI components for project settings
    private Text projectNameText;
    private Text toolchainPathText;
    private Text cflagsText; // CFLAGS input field
    private String boardModel;

    protected ProjectSettingsPage(String pageName) {
        super(pageName);
        setTitle("Project Settings");
        setDescription("Set the project name and toolchain configuration.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(3, false));

        // Project Name
        new Label(container, SWT.NULL).setText("Project Name:");
        projectNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        projectNameText.addModifyListener(e -> validatePage());

        // Toolchain Path
        new Label(container, SWT.NULL).setText("Toolchain Path:");
        toolchainPathText = new Text(container, SWT.BORDER | SWT.SINGLE);
        toolchainPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolchainPathText.addModifyListener(e -> validatePage());

        Button browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addListener(SWT.Selection, e -> {
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            dialog.setText("Select Toolchain Root Directory");
            dialog.setFilterPath(toolchainPathText.getText());
            String path = dialog.open();
            if (path != null) {
                toolchainPathText.setText(path);
            }
        });

        // CFLAGS input
        new Label(container, SWT.NULL).setText("C Compiler Flags (CFLAGS):");
        cflagsText = new Text(container, SWT.BORDER | SWT.SINGLE);
        cflagsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        cflagsText.setText("-Wall -O2 -march=rv64imac -mabi=lp64"); // Set default value

        validatePage();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            NewProjectWizard wizard = (NewProjectWizard) getWizard();
            this.boardModel = wizard.getBoardModel();
            updateToolchainPath();
            validatePage();
        }
    }

    private void updateToolchainPath() {
        String detectedPath = ToolchainLocator.findToolchainPathForBoard(this.boardModel);
        if (detectedPath != null) {
            toolchainPathText.setText(detectedPath);
        } else {
            String lastUsed = ToolchainLocator.getLastUsedPath();
            toolchainPathText.setText(lastUsed != null ? lastUsed : "");
        }
    }

    private void validatePage() {
        String projectName = projectNameText.getText().trim();
        if (projectName.isEmpty()) {
            setErrorMessage("Project Name cannot be empty.");
            setPageComplete(false);
            return;
        }
        IStatus status = ResourcesPlugin.getWorkspace().validateName(projectName, IStatus.ERROR);
        if (!status.isOK()) {
            setErrorMessage("Project name contains invalid characters.");
            setPageComplete(false);
            return;
        }
        if (ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).exists()) {
            setErrorMessage("Project \"" + projectName + "\" already exists.");
            setPageComplete(false);
            return;
        }
        if (toolchainPathText.getText().trim().isEmpty()) {
            setErrorMessage("Toolchain Path cannot be empty.");
            setPageComplete(false);
            return;
        }
        setErrorMessage(null);
        setPageComplete(true);
    }

    public String getProjectName() {
        return projectNameText.getText().trim();
    }

    public String getToolchainPath() {
        return toolchainPathText.getText().trim();
    }

    /**
     * Returns the CFLAGS entered by the user.
     * 
     * @return The C compiler flags.
     */
    public String getCFlags() {
        if (cflagsText != null && !cflagsText.isDisposed()) {
            return cflagsText.getText();
        }
        return "";
    }
}
