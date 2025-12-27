package org.ruyisdk.venv.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.ruyisdk.ruyi.util.RuyiLogger;
import org.ruyisdk.venv.Activator;
import org.ruyisdk.venv.model.Venv;
import org.ruyisdk.venv.model.VenvConfigurationService;
import org.ruyisdk.venv.model.VenvDetectionService;

/**
 * View model for the venv list view.
 */
public class VenvListViewModel {

    private static final RuyiLogger LOGGER = Activator.getLogger();

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean isFetching = false;
    private boolean canDelete = false;
    private boolean canApply = false;

    private final VenvDetectionService detectionService;
    private final VenvConfigurationService configService;
    private final IObservableList<Venv> observableVenvList = new WritableList<>(new ArrayList<>(), Venv.class);
    private final IObservableList<Venv> selectedVenvs = new WritableList<>(new ArrayList<>(), Venv.class);

    /**
     * Creates a new view model.
     *
     * @param detectionService the venv detection service
     * @param configService the configuration service
     */
    public VenvListViewModel(VenvDetectionService detectionService, VenvConfigurationService configService) {
        this.detectionService = detectionService;
        this.configService = configService;
        selectedVenvs.addListChangeListener((IListChangeListener<Venv>) event -> {
            updateCanDelete();
            updateCanApply();
        });
    }

    /**
     * Returns the observable list of detected venvs.
     *
     * @return the observable venv list
     */
    public IObservableList<Venv> getVenvList() {
        return observableVenvList;
    }

    /**
     * Returns the observable list of selected venvs.
     *
     * @return the observable list of selected venvs
     */
    public IObservableList<Venv> getSelectedVenvs() {
        return selectedVenvs;
    }

    /**
     * Returns whether delete is currently allowed.
     *
     * @return whether delete is currently allowed
     */
    public boolean isCanDelete() {
        return canDelete;
    }

    private void setCanDelete(boolean canDelete) {
        if (this.canDelete == canDelete) {
            return;
        }
        pcs.firePropertyChange("canDelete", this.canDelete, this.canDelete = canDelete);
    }

    /**
     * Returns whether apply configuration is currently allowed.
     *
     * @return whether apply is currently allowed
     */
    public boolean isCanApply() {
        return canApply;
    }

    private void setCanApply(boolean canApply) {
        if (this.canApply == canApply) {
            return;
        }
        pcs.firePropertyChange("canApply", this.canApply, this.canApply = canApply);
    }

    /**
     * Adds a property change listener.
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    private void setFetching(boolean isFetching) {
        this.isFetching = isFetching;
        updateCanDelete();
        updateCanApply();
    }

    private void updateCanDelete() {
        setCanDelete(!isFetching && !getSelectedVenvDirectoryPaths().isEmpty());
    }

    private void updateCanApply() {
        // Can apply if exactly one venv is selected and it has a project path
        if (isFetching || selectedVenvs.size() != 1) {
            setCanApply(false);
            return;
        }
        final var selected = selectedVenvs.get(0);
        final var projectPath = selected.getProjectPath();
        setCanApply(projectPath != null && !projectPath.isEmpty());
    }

    /**
     * Triggers an asynchronous refresh of the detected venv list.
     */
    public void onRefreshVenvListAsync() {
        if (isFetching) {
            return;
        }

        LOGGER.logInfo("Refreshing venv list");

        setFetching(true);
        detectionService.detectProjectVenvsAsync(result -> {
            observableVenvList.getRealm().asyncExec(() -> {
                observableVenvList.clear();
                observableVenvList.addAll(result);
                LOGGER.logInfo("Venv list refreshed; count=" + result.size());
                setFetching(false);
            });
        });
    }

    /**
     * Returns the selected venv directory paths.
     *
     * @return the selected venv directory paths
     */
    public List<String> getSelectedVenvDirectoryPaths() {
        return detectionService.getVenvDirectoryPathsFromVenvs(new ArrayList<>(selectedVenvs));
    }

    /**
     * Deletes the selected venv directories.
     *
     * @param callback callback invoked with an error, or {@code null} on success
     */
    public void onDeleteSelectedVenvDirectories(Consumer<Exception> callback) {
        if (isFetching) {
            return;
        }

        final var venvPaths = getSelectedVenvDirectoryPaths();
        if (venvPaths.isEmpty()) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }

        LOGGER.logInfo("Deleting selected venv directories: count=" + venvPaths.size());

        setFetching(true);
        detectionService.deleteVenvDirectoriesAsync(venvPaths, err -> {
            observableVenvList.getRealm().asyncExec(() -> {
                setFetching(false);
                if (err == null) {
                    onRefreshVenvListAsync();
                }
                if (callback != null) {
                    callback.accept(err);
                }
            });
        });
    }

    /**
     * Applies the selected venv's configuration to its associated project.
     *
     * @param callback callback invoked with the result
     */
    public void onApplySelectedVenvConfig(Consumer<VenvConfigurationService.ApplyResult> callback) {
        if (isFetching) {
            if (callback != null) {
                callback.accept(new VenvConfigurationService.ApplyResult(false, "Operation in progress"));
            }
            return;
        }

        if (selectedVenvs.size() != 1) {
            if (callback != null) {
                callback.accept(new VenvConfigurationService.ApplyResult(false, "No venv selected"));
            }
            return;
        }

        final var selected = selectedVenvs.get(0);
        LOGGER.logInfo("Applying venv configuration to project: venv=" + selected.getPath());
        setFetching(true);
        configService.applyToProjectAsync(selected, result -> {
            observableVenvList.getRealm().asyncExec(() -> {
                setFetching(false);
                if (result.isSuccess()) {
                    LOGGER.logInfo("Venv configuration applied successfully");
                } else {
                    LOGGER.logWarning("Venv configuration failed: " + result.getMessage(), null);
                }
                if (callback != null) {
                    callback.accept(result);
                }
            });
        });
    }
}
