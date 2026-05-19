package org.ruyisdk.venv.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.venv.Activator;
import org.ruyisdk.venv.viewmodel.ProjectVenvContextMenuViewModel;
import org.ruyisdk.venv.views.VenvWizard;

/**
 * Handler opening the venv creation wizard from a project context menu.
 */
public class NewProjectVenvHandler extends AbstractHandler {

    private static final PluginLogger LOGGER = Activator.getLogger();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final var selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        final var firstElement = selection.getFirstElement();
        final var project = firstElement instanceof IProject ? (IProject) firstElement
                : ((IAdaptable) firstElement).getAdapter(IProject.class);
        final var projectPath = project.getLocation().toOSString();

        LOGGER.logInfo("Opening new venv wizard from project context: project=" + projectPath);

        final var activator = Activator.getDefault();
        final var viewModel = new ProjectVenvContextMenuViewModel(activator.getDetectionService(),
                activator.getConfigService());
        final var wizardViewModel = viewModel.createWizardViewModel(projectPath);
        final var dialog = new WizardDialog(HandlerUtil.getActiveShell(event),
                new VenvWizard(wizardViewModel));
        dialog.open();
        return null;
    }
}
