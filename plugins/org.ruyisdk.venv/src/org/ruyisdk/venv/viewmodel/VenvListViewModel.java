package org.ruyisdk.venv.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.ruyisdk.ruyi.core.workspace.WorkspaceProjectsMonitor;
import org.ruyisdk.ruyi.core.workspace.WorkspaceProjectsMonitor.EventKind;
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

    private boolean fetching = false;
    private boolean canDelete = false;
    private boolean canApply = false;
    private boolean hasOpenProjects = false;
    private boolean refreshScheduled = false;
    private boolean busy = false;
    private String tableAreaMessage = "uninitialized.";

    private boolean disposed = false;
    private final WorkspaceProjectsMonitor.Listener workspaceProjectsListener;

    private final WorkspaceProjectsMonitor workspaceProjectsMonitor;
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
        this.workspaceProjectsMonitor = WorkspaceProjectsMonitor.getInstance();

        updateHasOpenProjects();
        updateDerivedState();

        workspaceProjectsListener = new WorkspaceProjectsMonitor.Listener() {
            @Override
            public void onWorkspaceProjectsEvent(WorkspaceProjectsMonitor.Event event) {
                if (event == null || disposed) {
                    return;
                }
                observableVenvList.getRealm().asyncExec(() -> {
                    if (disposed) {
                        return;
                    }
                    if (event.getKind() == EventKind.PROJECTS_CHANGED) {
                        setHasOpenProjects(event.hasOpenProjects());
                        observableVenvList.clear();
                        selectedVenvs.clear();
                        setRefreshScheduled(event.hasOpenProjects());
                        updateDerivedState();
                        return;
                    } else if (event.getKind() == EventKind.DEBOUNCE_TRIGGERED) {
                        onRefreshVenvListAsync();
                        return;
                    }
                });
            }
        };
        this.workspaceProjectsMonitor.addListener(workspaceProjectsListener);

        selectedVenvs.addListChangeListener(e -> {
            updateActionStates();
        });

        observableVenvList.addListChangeListener(e -> {
            updateDerivedState();
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
        if (this.fetching == isFetching) {
            return;
        }
        this.fetching = isFetching;
        if (this.fetching) {
            setRefreshScheduled(false);
        }
        updateDerivedState();
    }

    private void setHasOpenProjects(boolean hasOpenProjects) {
        if (this.hasOpenProjects == hasOpenProjects) {
            return;
        }
        this.hasOpenProjects = hasOpenProjects;
    }

    private void setRefreshScheduled(boolean refreshScheduled) {
        if (this.refreshScheduled == refreshScheduled) {
            return;
        }
        this.refreshScheduled = refreshScheduled;
    }

    public boolean isBusy() {
        return busy;
    }

    private void setBusy(boolean busy) {
        if (this.busy == busy) {
            return;
        }
        pcs.firePropertyChange("busy", this.busy, this.busy = busy);
    }

    private void updateDerivedState() {
        final var busyNow = fetching || refreshScheduled;
        setBusy(busyNow);
        updateTableAreaPresentation();
        updateActionStates();
    }

    private void updateActionStates() {
        setCanDelete(!busy && !getSelectedVenvDirectoryPaths().isEmpty());

        // Can apply if exactly one venv is selected and it has a project path
        if (busy || selectedVenvs.size() != 1) {
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
        if (fetching) {
            return;
        }

        updateHasOpenProjects();
        if (!hasOpenProjects) {
            observableVenvList.getRealm().asyncExec(() -> {
                observableVenvList.clear();
                selectedVenvs.clear();
                setRefreshScheduled(false);
                updateDerivedState();
            });
            return;
        }

        LOGGER.logInfo("Refreshing venv list");

        setFetching(true);
        final var projectRootPaths = workspaceProjectsMonitor.getOpenProjectRootPaths();
        detectionService.detectProjectVenvsAsync(projectRootPaths, result -> {
            observableVenvList.getRealm().asyncExec(() -> {
                observableVenvList.clear();
                observableVenvList.addAll(result);
                LOGGER.logInfo("Venv list refreshed; count=" + result.size());
                updateHasOpenProjects();
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
        if (busy) {
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
        if (busy) {
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

    private void setTableAreaMessage(String tableAreaMessage) {
        final var newValue = tableAreaMessage == null ? "" : tableAreaMessage;
        if (newValue.equals(this.tableAreaMessage)) {
            return;
        }
        pcs.firePropertyChange("tableAreaMessage", this.tableAreaMessage, this.tableAreaMessage = newValue);
    }

    private void updateTableAreaPresentation() {
        if (!hasOpenProjects) {
            setTableAreaMessage("No open projects.");
            return;
        }

        if (busy) {
            setTableAreaMessage("Detecting virtual environments...");
            return;
        }

        if (observableVenvList.isEmpty()) {
            setTableAreaMessage("No venv detected in open projects.");
            return;
        }

        // Setting empty message to show the table
        setTableAreaMessage("");
    }

    private void updateHasOpenProjects() {
        setHasOpenProjects(workspaceProjectsMonitor.hasOpenProjects());
    }

    /**
     * Returns root paths for currently-open projects.
     *
     * @return open project root paths
     */
    public List<String> getOpenProjectRootPaths() {
        return workspaceProjectsMonitor.getOpenProjectRootPaths();
    }

    /**
     * Returns the hint text shown in the table area when the table is hidden.
     *
     * @return the hint text
     */
    public String getTableAreaMessage() {
        return tableAreaMessage;
    }

    /**
     * Disposes resources held by this view model.
     */
    public void dispose() {
        disposed = true;
        workspaceProjectsMonitor.removeListener(workspaceProjectsListener);
    }
}
