package org.ruyisdk.packages.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.function.Consumer;

/**
 * Base class for ViewModels, providing {@link PropertyChangeSupport}-based property-change
 * notification and a {@code uiExecutor} for marshalling work to the UI thread.
 *
 * <p>
 * Listener registration and removal are synchronous so that listeners are guaranteed to be
 * registered before any subsequent {@link #firePropertyChange} call can deliver events.
 * {@link #firePropertyChange} marshals delivery to the UI thread via {@code uiExecutor}, so callers
 * do <em>not</em> need to wrap their own calls in {@code uiExecutor.accept(...)}.
 */
public abstract class BaseViewModel {

    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /** Executor that posts a {@link Runnable} to the UI thread. */
    protected final Consumer<Runnable> uiExecutor;

    /**
     * Creates a ViewModel with the given UI-thread executor.
     *
     * @param uiExecutor posts a {@link Runnable} to the UI thread; e.g.
     *        {@code Display.getDefault()::asyncExec}
     */
    protected BaseViewModel(Consumer<Runnable> uiExecutor) {
        this.uiExecutor = uiExecutor;
    }

    /** Register a listener for all property changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /** Register a listener for a specific property. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /** Remove a previously registered listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /** Remove a previously registered per-property listener. */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /** Fire a property-change event on the UI thread. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        uiExecutor.accept(() -> pcs.firePropertyChange(propertyName, oldValue, newValue));
    }
}
