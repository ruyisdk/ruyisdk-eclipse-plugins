package org.ruyisdk.venv.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.venv.Activator;
import org.ruyisdk.venv.model.Venv;
import org.ruyisdk.venv.viewmodel.ProjectVenvContextMenuViewModel;

/**
 * Handler applying a selected venv from the project context menu.
 */
public class ApplyProjectVenvHandler extends AbstractHandler {

    private static final PluginLogger LOGGER = Activator.getLogger();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final var selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        final var firstElement = selection.getFirstElement();
        final var project = firstElement instanceof IProject ? (IProject) firstElement
                : ((IAdaptable) firstElement).getAdapter(IProject.class);
        final var projectPath = project.getLocation().toOSString();

        final var activator = Activator.getDefault();
        final var viewModel = new ProjectVenvContextMenuViewModel(activator.getDetectionService(),
                activator.getConfigService());
        final var candidates = viewModel.listProjectVenvs(projectPath);

        final var dialog = new ElementListSelectionDialog(HandlerUtil.getActiveShell(event),
                new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        return ProjectVenvContextMenuViewModel
                                .toApplyVenvDisplayText((Venv) element);
                    }
                });
        dialog.setTitle("Apply venv");
        dialog.setMessage(
                "Select a virtual environment to apply to project \"" + project.getName() + "\".");
        dialog.setElements(candidates.toArray(Venv[]::new));
        dialog.setMultipleSelection(false);

        if (dialog.open() != Window.OK) {
            return null;
        }

        final var selectedVenv = (Venv) dialog.getFirstResult();
        final var result = viewModel.applyVenv(selectedVenv);

        LOGGER.logInfo("Applied venv from project context menu: project=" + projectPath + ", venv="
                + selectedVenv.getPath());
        MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Apply venv",
                result.getMessage());
        return null;
    }
}
