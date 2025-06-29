package org.ruyisdk.projectcreator.wizards;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.ruyisdk.projectcreator.utils.ToolchainLocator;

public class ProjectSettingsPage extends WizardPage {

    private Text projectNameText;
    private Text toolchainPathText;
    private String boardModel;

    protected ProjectSettingsPage(String pageName) {
        super(pageName);
        setTitle("project Settings");
        setDescription("project Settings Page Description");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(3, false));

        new Label(container, SWT.NULL).setText("projectName:");
        projectNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        projectNameText.addModifyListener(e -> validatePage());

        new Label(container, SWT.NULL).setText("toolchainPath:");
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
            setErrorMessage("project Name Cannot Be Empty");
            setPageComplete(false);
            return;
        }
        IStatus status = ResourcesPlugin.getWorkspace().validateName(projectName, IStatus.ERROR);
        if (!status.isOK()) {
            setErrorMessage("projectNameContainsInvalidCharacters");
            setPageComplete(false);
            return;
        }
        if (ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).exists()) {
            setErrorMessage("project\"" + projectName + "\" is already exists.");
            setPageComplete(false);
            return;
        }
        if (toolchainPathText.getText().trim().isEmpty()) {
            setErrorMessage("toolchainPath Cannot be empty");
            setPageComplete(false);
            return;
        }
        setErrorMessage(null);
        setPageComplete(true);
    }

    public String getProjectName() { return projectNameText.getText().trim(); }
    public String getToolchainPath() { return toolchainPathText.getText().trim(); }
}