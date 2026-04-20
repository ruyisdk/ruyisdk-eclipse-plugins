package org.ruyisdk.venv.views;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;
import org.ruyisdk.core.exception.PluginException;
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
        viewModel.loadAll();

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
        try {
            getContainer().run(true, true, monitor -> {
                try {
                    monitor.subTask("install toolchain");
                    viewModel.installToolchain();

                    if (viewModel.isEmulatorEnabled()) {
                        monitor.subTask("install emulator");
                        viewModel.installEmulator();
                    }

                    monitor.subTask("create venv");
                    viewModel.createVenv();
                } catch (PluginException e) {
                    throw new InvocationTargetException(e);
                }
            });
            return true;
        } catch (InvocationTargetException e) {
            StatusManager.getManager()
                    .handle(new Status(IStatus.ERROR, "org.ruyisdk.venv",
                            "Unable to complete virtual environment setup.", e.getCause()),
                            StatusManager.LOG | StatusManager.BLOCK);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            StatusManager.getManager().handle(
                    new Status(IStatus.CANCEL, "org.ruyisdk.venv", "Operation was cancelled.", e),
                    StatusManager.LOG | StatusManager.BLOCK);
            return false;
        }
    }
}
