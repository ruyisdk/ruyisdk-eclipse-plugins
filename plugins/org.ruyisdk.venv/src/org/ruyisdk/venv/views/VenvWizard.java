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

    private WizardLoadingPage loadingPage;
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
        loadingPage = new WizardLoadingPage(viewModel);
        addPage(loadingPage);
        configurationPage = new WizardConfigPage(viewModel);
        addPage(configurationPage);
        locationPage = new WizardLocationPage(viewModel);
        addPage(locationPage);
    }

    @Override
    public boolean canFinish() {
        return locationPage != null && locationPage.isPageComplete()
                && (getContainer() != null && getContainer().getCurrentPage() == locationPage);
    }

    @Override
    public boolean performFinish() {
        // Prepare data here, then use data in the worker thread.
        // Forked runnable must not touch observables.
        try {
            viewModel.buildFinalizationData();
        } catch (PluginException e) {
            StatusManager.getManager()
                    .handle(new Status(IStatus.ERROR, "org.ruyisdk.venv",
                            "Unable to complete virtual environment setup.", e),
                            StatusManager.LOG | StatusManager.BLOCK);
            return false;
        }
        try {
            getContainer().run(true, true, monitor -> {
                try {
                    viewModel.doFinalization(monitor::subTask);
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
