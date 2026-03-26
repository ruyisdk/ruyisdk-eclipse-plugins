package org.ruyisdk.packages.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.function.Consumer;

/**
 * Base class for ViewModels, providing {@link PropertyChangeSupport}-based property-change
 * notification and a {@code uiExecutor} for marshalling work to the UI thread.
 *
 * <p>
 * Subclasses are responsible for wrapping {@link #firePropertyChange} calls inside
 * {@code uiExecutor.accept(...)} when firing from a background thread, so that listeners can safely
 * update widgets.
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
        uiExecutor.accept(() -> pcs.addPropertyChangeListener(listener));
    }

    /** Register a listener for a specific property. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        uiExecutor.accept(() -> pcs.addPropertyChangeListener(propertyName, listener));
    }

    /** Remove a previously registered listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        uiExecutor.accept(() -> pcs.removePropertyChangeListener(listener));
    }

    /** Remove a previously registered per-property listener. */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        uiExecutor.accept(() -> pcs.removePropertyChangeListener(propertyName, listener));
    }

    /** Fire a property-change event. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        uiExecutor.accept(() -> pcs.firePropertyChange(propertyName, oldValue, newValue));
    }
}
