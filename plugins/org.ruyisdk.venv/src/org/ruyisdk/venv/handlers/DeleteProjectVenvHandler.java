package org.ruyisdk.venv.handlers;

import java.util.Arrays;
import java.util.List;
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
import org.ruyisdk.venv.viewmodel.VenvListViewModel;

/**
 * Handler deleting a venv selected from the project context menu.
 */
public class DeleteProjectVenvHandler extends AbstractHandler {

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

        if (candidates.isEmpty()) {
            MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Delete venv",
                    String.format("No virtual environments found under project \"%s\".",
                            project.getName()));
            return null;
        }

        final var dialog = new ElementListSelectionDialog(HandlerUtil.getActiveShell(event),
                new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        return VenvListViewModel.getDisplayPath((Venv) element);
                    }
                });
        dialog.setTitle("Delete venv");
        dialog.setMessage(String.format(
                "Select virtual environment(s) to delete from project \"%s\":", project.getName()));
        dialog.setElements(candidates.toArray(Venv[]::new));
        dialog.setMultipleSelection(true);

        if (dialog.open() != Window.OK) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final List<Venv> selectedVenvs = (List<Venv>) (List<?>) Arrays.asList(dialog.getResult());
        if (selectedVenvs.isEmpty()) {
            return null;
        }

        final var venvPaths =
                activator.getDetectionService().getVenvDirectoryPathsFromVenvs(selectedVenvs);

        final String message;
        if (venvPaths.size() == 1) {
            message = String.format("""
                This will delete the whole directory of the virtual environment:
                %s

                Continue?""", venvPaths.get(0));
        } else {
            message = String.format("""
                This will delete the whole directories of the selected virtual environments:
                %s

                Continue?""", String.join("\n", venvPaths));
        }

        final var confirmed = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event),
                "Delete virtual environment", message);
        if (!confirmed) {
            return null;
        }

        LOGGER.logInfo(
                String.format("Deleting venv(s) from project context menu: project=%s, count=%d",
                        projectPath, selectedVenvs.size()));
        viewModel.deleteVenvs(selectedVenvs);
        return null;
    }
}
