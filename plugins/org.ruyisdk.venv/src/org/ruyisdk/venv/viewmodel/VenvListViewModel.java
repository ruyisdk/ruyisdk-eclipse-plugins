package org.ruyisdk.venv.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.ruyisdk.venv.model.Venv;
import org.ruyisdk.venv.model.VenvService;

/**
 * View model for the venv list view.
 */
public class VenvListViewModel {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean isFetching = false;
    private boolean canDelete = false;

    private final VenvService service;
    private final IObservableList<Venv> observableVenvList = new WritableList<Venv>(new ArrayList<Venv>(), Venv.class);
    private final IObservableList<Venv> selectedVenvs = new WritableList<Venv>(new ArrayList<Venv>(), Venv.class);


    /**
     * Creates a new view model.
     *
     * @param service the venv service
     */

    public VenvListViewModel(VenvService service) {
        this.service = service;
        selectedVenvs.addListChangeListener((IListChangeListener<Venv>) event -> updateCanDelete());
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
    }


    private void updateCanDelete() {
        setCanDelete(!isFetching && !getSelectedVenvDirectoryPaths().isEmpty());
    }


    /**
     * Triggers an asynchronous refresh of the detected venv list.
     */

    public void onRefreshVenvList() {
        if (isFetching) {
            return;
        }

        setFetching(true);
        service.detectProjectVenvsAsync(result -> {
            observableVenvList.getRealm().asyncExec(() -> {
                observableVenvList.clear();
                observableVenvList.addAll(result);
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
        return service.getVenvDirectoryPathsFromVenvs(new ArrayList<>(selectedVenvs));
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

        final List<String> venvPaths = getSelectedVenvDirectoryPaths();
        if (venvPaths.isEmpty()) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }

        setFetching(true);
        service.deleteVenvDirectoriesAsync(venvPaths, err -> {
            observableVenvList.getRealm().asyncExec(() -> {
                setFetching(false);
                if (err == null) {
                    onRefreshVenvList();
                }
                if (callback != null) {
                    callback.accept(err);
                }
            });
        });
    }
}
