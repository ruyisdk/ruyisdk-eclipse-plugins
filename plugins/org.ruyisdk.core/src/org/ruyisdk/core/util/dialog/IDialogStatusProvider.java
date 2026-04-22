package org.ruyisdk.core.util.dialog;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;

/**
 * Provides observable status updates for dialog presentation.
 */
public interface IDialogStatusProvider {

    /**
     * Returns the observable that publishes the latest user-facing status.
     *
     * <p>
     * Consumers treat non-null value changes as dialog events.
     *
     * @return observable latest status, usually initialized with {@code null}
     */
    public IObservableValue<IStatus> getLastStatus();
}
