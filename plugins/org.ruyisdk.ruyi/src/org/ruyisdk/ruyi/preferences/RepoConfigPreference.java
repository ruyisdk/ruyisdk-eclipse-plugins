package org.ruyisdk.ruyi.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.core.config.Constants;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * Preference component for remote repository selection.
 */
public class RepoConfigPreference {

    private final Composite parent;
    private Button githubRadio;
    private Button iscasRadio;
    private Button customRadio;
    private Text customUrlText;
    private Text branchText;

    /**
     * Constructs preference component.
     *
     * @param parent parent composite
     */
    public RepoConfigPreference(Composite parent) {
        this.parent = parent;
    }

    /**
     * Creates UI section with its own group.
     */
    public void createSection() {
        final var group = new Group(parent, SWT.NONE);
        group.setText("Remote Repository");
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        createControls(group);
    }

    private void createControls(Composite container) {
        var currentUrl = RuyiCli.getRepoRemote();
        if (currentUrl == null || currentUrl.isBlank()) {
            currentUrl = Constants.NetAddress.MAIN_REPO_URL;
        }
        currentUrl = currentUrl.trim();

        final var isGithub = currentUrl.equals(Constants.NetAddress.MAIN_REPO_URL);
        final var isIscas = currentUrl.equals(Constants.NetAddress.BACKUP_REPO_URL);
        final var isCustom = !isGithub && !isIscas;

        // GitHub

        githubRadio = new Button(container, SWT.RADIO);
        githubRadio.setText("GitHub:");
        githubRadio.setSelection(isGithub);

        final var githubUrl = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        githubUrl.setText(Constants.NetAddress.MAIN_REPO_URL);
        githubUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        githubUrl.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        // ISCAS mirror

        iscasRadio = new Button(container, SWT.RADIO);
        iscasRadio.setText("ISCAS mirror:");
        iscasRadio.setSelection(isIscas);

        final var iscasUrl = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        iscasUrl.setText(Constants.NetAddress.BACKUP_REPO_URL);
        iscasUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        iscasUrl.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        // Custom

        customRadio = new Button(container, SWT.RADIO);
        customRadio.setText("Custom:");
        customRadio.setSelection(isCustom);

        customUrlText = new Text(container, SWT.BORDER);
        customUrlText.setText(isCustom ? currentUrl : "");
        customUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        customUrlText.setEnabled(isCustom);

        // Enable/disable custom text field based on radio selection

        githubRadio.addListener(SWT.Selection, e -> switchFromCustom(githubRadio));
        iscasRadio.addListener(SWT.Selection, e -> switchFromCustom(iscasRadio));
        customRadio.addListener(SWT.Selection, e -> {
            customUrlText.setEnabled(true);
            customUrlText.setFocus();
        });

        // Branch

        final var branchLabel = new Label(container, SWT.NONE);
        branchLabel.setText("Branch:");

        final var branchValue = RuyiCli.getRepoBranch();
        branchText = new Text(container, SWT.BORDER);
        branchText.setText(branchValue != null && !branchValue.isBlank() ? branchValue.trim()
                        : Constants.NetAddress.DEFAULT_BRANCH);
        branchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void switchFromCustom(Button selected) {
        if (!customUrlText.getText().trim().isEmpty()) {
            boolean confirmed = MessageDialog.openConfirm(parent.getShell(), "Deselect Custom Repository",
                            "The custom repository URL will be permanently removed. Continue?");
            if (!confirmed) {
                selected.setSelection(false);
                customRadio.setSelection(true);
                return;
            }
            customUrlText.setText("");
        }
        customUrlText.setEnabled(false);
    }

    /**
     * Returns the branch name entered by the user.
     *
     * @return branch name, defaults to {@value Constants.NetAddress#DEFAULT_BRANCH} if blank
     */
    public String getBranch() {
        final var text = branchText.getText().trim();
        return text.isEmpty() ? Constants.NetAddress.DEFAULT_BRANCH : text;
    }

    /**
     * Returns the URL of the currently selected remote repository.
     *
     * @return selected remote URL
     */
    public String getSelectedUrl() {
        if (iscasRadio.getSelection()) {
            return Constants.NetAddress.BACKUP_REPO_URL;
        }
        if (customRadio.getSelection()) {
            final var text = customUrlText.getText().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return Constants.NetAddress.MAIN_REPO_URL;
    }

    /**
     * Resets to default repository state (GitHub selected, custom URL cleared).
     */
    public void defaultedRepoState() {
        githubRadio.setSelection(true);
        iscasRadio.setSelection(false);
        customRadio.setSelection(false);
        customUrlText.setText("");
        customUrlText.setEnabled(false);
        branchText.setText(Constants.NetAddress.DEFAULT_BRANCH);
    }
}
