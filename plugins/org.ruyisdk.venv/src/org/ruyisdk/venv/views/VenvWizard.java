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
import org.ruyisdk.ruyi.services.RuyiCli;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * Wizard to create a new virtual environment.
 */
public class VenvWizard extends Wizard {
    private WizardConfigPage configurationPage;
    private WizardLocationPage locationPage;
    private final VenvWizardViewModel viewModel;

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
            ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
            if (pmd.getShell() != null) {
                pmd.getShell().setText("Updating package index");
            }
            pmd.run(true, true, monitor -> {
                monitor.beginTask("Updating package index...", 100);
                RuyiCli.RunResult ur = viewModel.updateIndex();
                if (ur == null) {
                    throw new InvocationTargetException(new Exception("Update returned no result"));
                }
                if (ur.getExitCode() != 0) {
                    throw new InvocationTargetException(new Exception(ur.getOutput() == null ? "" : ur.getOutput()));
                }
                monitor.worked(100);
            });
        } catch (InvocationTargetException e) {
            String detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            openSelectableError("Package Index Update Failed", "Failed to update package index; wizard will abort.",
                            detail == null ? "" : detail);
            return;
        } catch (InterruptedException e) {
            openSelectableError("Package Index Update Cancelled", "Update was cancelled; wizard will abort.", "");
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
        IRunnableWithProgress op = monitor -> {
            monitor.subTask("install toolchain");
            RuyiCli.RunResult rc = viewModel.installToolchain();
            if (rc.getExitCode() != 0) {
                throw new InvocationTargetException(new Exception(
                                "Failed to install toolchain:" + System.lineSeparator() + rc.getOutput()));
            }

            if (viewModel.isEmulatorEnabled()) {
                monitor.subTask("install emulator");
                RuyiCli.RunResult erc = viewModel.installEmulator();
                if (erc.getExitCode() != 0) {
                    throw new InvocationTargetException(new Exception(
                                    "Failed to install emulator:" + System.lineSeparator() + erc.getOutput()));
                }
            }

            monitor.subTask("create venv");
            RuyiCli.RunResult vrc = viewModel.createVenv();
            if (vrc.getExitCode() != 0) {
                throw new InvocationTargetException(new Exception("Failed to create venv, exit=" + vrc.getExitCode()
                                + System.lineSeparator() + vrc.getOutput()));
            }
        };

        try {
            getContainer().run(true, true, op);
            return true;
        } catch (InvocationTargetException e) {
            String detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (detail == null) {
                detail = "";
            }
            String top = "Unable to complete virtual environment setup — see details below.";
            openSelectableError("Virtual Environment Setup Failed", top, detail);
            return false;
        } catch (InterruptedException e) {
            MessageDialog.openError(getShell(), "Cancelled", "Operation was cancelled");
            return false;
        }
    }

    private void openSelectableError(String title, String message, String details) {
        Dialog dlg = new Dialog(getShell()) {
            @Override
            protected Control createDialogArea(Composite parent) {
                Composite comp = (Composite) super.createDialogArea(parent);
                comp.setLayout(new GridLayout(1, false));

                Text msgText = new Text(comp, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
                msgText.setText(message == null ? "" : message);
                GridData mgd = new GridData(GridData.FILL_HORIZONTAL);
                mgd.heightHint = 80;
                msgText.setLayoutData(mgd);

                Label dlabel = new Label(comp, SWT.NONE);
                dlabel.setText("Details:");

                Text detailsText = new Text(comp, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
                detailsText.setText(details == null ? "" : details);
                GridData dgd = new GridData(GridData.FILL_BOTH);
                dgd.heightHint = 240;
                detailsText.setLayoutData(dgd);

                return comp;
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
        dlg.open();
    }
}
