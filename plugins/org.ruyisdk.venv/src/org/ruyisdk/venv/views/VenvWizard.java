package org.ruyisdk.venv.views;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * Wizard to create a new virtual environment.
 */
public class VenvWizard extends Wizard {
    private final VenvWizardViewModel viewModel;

    private WizardConfigPage configurationPage;
    private WizardLocationPage locationPage;

    /**
     * Creates a wizard.
     *
     * @param viewModel the view model
     */
    public VenvWizard(VenvWizardViewModel viewModel) {
        super();
        this.viewModel = viewModel;
        setNeedsProgressMonitor(true);
        setWindowTitle("New virtual environment");
    }

    @Override
    public void addPages() {
        try {
            final var pmd = new ProgressMonitorDialog(getShell());
            if (pmd.getShell() != null) {
                pmd.getShell().setText("Updating package index");
            }
            pmd.run(true, true, monitor -> {
                monitor.beginTask("Updating package index...", 100);
                final var result = viewModel.updateIndex();
                if (result == null) {
                    throw new InvocationTargetException(new Exception("Update returned no result"));
                }
                if (result.getExitCode() != 0) {
                    throw new InvocationTargetException(new Exception(result.getOutput()));
                }
                monitor.worked(100);
            });
        } catch (InvocationTargetException e) {
            final var detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            displaySelectableError("Package Index Update Failed", "Failed to update package index; wizard will abort.",
                            detail == null ? "" : detail);
            return;
        } catch (InterruptedException e) {
            displaySelectableError("Package Index Update Cancelled", "Update was cancelled; wizard will abort.", "");
            return;
        }

        configurationPage = new WizardConfigPage(viewModel);
        addPage(configurationPage);
        locationPage = new WizardLocationPage(viewModel);
        addPage(locationPage);
    }

    @Override
    public boolean canFinish() {
        return locationPage != null && locationPage.isPageComplete();
    }

    @Override
    public boolean performFinish() {
        final IRunnableWithProgress operation = monitor -> {
            monitor.subTask("install toolchain");
            {
                final var result = viewModel.installToolchain();
                if (result.getExitCode() != 0) {
                    throw new InvocationTargetException(
                                    new Exception("Failed to install toolchain:\n" + result.getOutput()));
                }
            }

            if (viewModel.isEmulatorEnabled()) {
                monitor.subTask("install emulator");
                final var result = viewModel.installEmulator();
                if (result.getExitCode() != 0) {
                    throw new InvocationTargetException(
                                    new Exception("Failed to install emulator:\n" + result.getOutput()));
                }
            }

            monitor.subTask("create venv");
            {
                final var result = viewModel.createVenv();
                if (result.getExitCode() != 0) {
                    final var errorMessage =
                                    "Failed to create venv, exit=" + result.getExitCode() + "\n" + result.getOutput();
                    throw new InvocationTargetException(new Exception(errorMessage));
                }
            }
        };

        try {
            getContainer().run(true, true, operation);
            return true;
        } catch (InvocationTargetException e) {
            var detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (detail == null) {
                detail = "";
            }
            final var msg = "Unable to complete virtual environment setup — see details below.";
            displaySelectableError("Virtual Environment Setup Failed", msg, detail);
            return false;
        } catch (InterruptedException e) {
            MessageDialog.openError(getShell(), "Cancelled", "Operation was cancelled");
            return false;
        }
    }

    private void displaySelectableError(String title, String message, String details) {
        final var dialog = new Dialog(getShell()) {
            @Override
            protected Control createDialogArea(Composite parent) {
                final var composite = (Composite) super.createDialogArea(parent);
                composite.setLayout(new GridLayout(1, false));

                final var msgText = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
                {
                    var gridData = new GridData(GridData.FILL_HORIZONTAL);
                    gridData.heightHint = 80;
                    msgText.setLayoutData(gridData);
                }
                msgText.setText(message == null ? "" : message);

                final var detailsLabel = new Label(composite, SWT.NONE);
                detailsLabel.setText("Details:");

                final var detailsText = new Text(composite,
                                SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
                {
                    var gridData = new GridData(GridData.FILL_HORIZONTAL);
                    gridData.heightHint = 120;
                    detailsText.setLayoutData(gridData);
                }
                detailsText.setText(details == null ? "" : details);

                return composite;
            }

            @Override
            protected void configureShell(Shell newShell) {
                super.configureShell(newShell);
                newShell.setText(title == null ? "Error" : title);
            }

            @Override
            protected boolean isResizable() {
                return true;
            }

            @Override
            protected void createButtonsForButtonBar(Composite parent) {
                createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            }
        };
        dialog.open();
    }
}
