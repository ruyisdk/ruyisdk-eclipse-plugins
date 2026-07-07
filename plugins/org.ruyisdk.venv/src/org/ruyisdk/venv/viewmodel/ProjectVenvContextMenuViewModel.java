package org.ruyisdk.venv.viewmodel;

import java.util.List;
import org.ruyisdk.venv.model.Venv;
import org.ruyisdk.venv.model.VenvConfigurationService;
import org.ruyisdk.venv.model.VenvDetectionService;

/**
 * View model backing venv actions in project context menus.
 */
public class ProjectVenvContextMenuViewModel {

    private final VenvDetectionService detectionService;
    private final VenvConfigurationService configService;

    /**
     * Creates a new view model.
     *
     * @param detectionService venv detection service
     * @param configService venv configuration service
     */
    public ProjectVenvContextMenuViewModel(VenvDetectionService detectionService,
            VenvConfigurationService configService) {
        this.detectionService = detectionService;
        this.configService = configService;
    }

    /**
     * Creates a wizard view model for the selected project.
     *
     * @param projectPath selected project path
     * @return preconfigured wizard view model
     */
    public VenvWizardViewModel createWizardViewModel(String projectPath) {
        final var wizardViewModel = new VenvWizardViewModel(detectionService);
        wizardViewModel.setProjectRootPaths(List.of(projectPath));
        wizardViewModel.setVenvLocation(projectPath);
        wizardViewModel.setVenvLocationReadOnly(true);
        return wizardViewModel;
    }

    /**
     * Lists detected venvs under the selected project.
     *
     * @param projectPath selected project path
     * @return detected venvs for the selected project
     */
    public List<Venv> listProjectVenvs(String projectPath) {
        return detectionService.detectProjectVenvsAsync(List.of(projectPath)).join();
    }

    /**
     * Applies the selected venv to its associated project.
     *
     * @param venv selected venv
     * @return apply result
     */
    public VenvConfigurationService.ApplyResult applyVenv(Venv venv) {
        return configService.applyToProjectAsync(venv).join();
    }

    /**
     * Deletes the given venvs.
     *
     * @param venvs venvs to delete
     */
    public void deleteVenvs(List<Venv> venvs) {
        final var venvPaths = detectionService.getVenvDirectoryPathsFromVenvs(venvs);
        detectionService.deleteVenvDirectoriesAsync(venvPaths).join();
    }

    /**
     * Returns display text for the apply-venv selection list.
     *
     * @param venv venv row
     * @return display text
     */
    public static String toApplyVenvDisplayText(Venv venv) {
        return String.format("%s | profile=%s | toolchain=%s | qemu=%s",
                VenvListViewModel.getDisplayPath(venv), venv.getProfile(),
                venv.getToolchainPrefix(),
                venv.getEmulatorExecutableName().isEmpty() ? "No" : "Yes");
    }
}
