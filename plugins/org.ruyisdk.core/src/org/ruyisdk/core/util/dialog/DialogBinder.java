package org.ruyisdk.core.util.dialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Subscribes to provider status updates and displays them as message dialogs.
 *
 * <p>
 * Each non-null status change triggers a dialog on the UI thread.
 */
public class DialogBinder {

    private static final String errorSuffix = "See the \"Error Log\" view for details.";

    record DialogSpec(int kind, String message) {
    }

    /**
     * Attaches a listener to {@link IDialogStatusProvider#getLastStatus()}.
     *
     * <p>
     * The status severity is mapped to the dialog kind (error, warning, information, or default).
     * For every dialog attempt, the method also tries to open the Eclipse Error Log view first.
     *
     * @param shell parent shell used to display dialogs
     * @param provider source of status updates
     * @param title dialog title
     */
    public static void bind(Shell shell, IDialogStatusProvider provider, String title) {
        provider.getLastStatus().addValueChangeListener(ev -> {
            final var s = ev.diff.getNewValue();
            if (s != null) {
                final var spec = constructSpec(s);

                shell.getDisplay().asyncExec(() -> {
                    {
                        final var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            final var page = window.getActivePage();
                            if (page != null) {
                                try {
                                    page.showView("org.eclipse.pde.runtime.LogView");
                                } catch (PartInitException e) {
                                    // ignore
                                }
                            }
                        }
                    }
                    MessageDialog.open(spec.kind, shell, title, spec.message, SWT.NONE);
                });
            }
        });
    }

    private static DialogSpec constructSpec(IStatus status) {
        var kind = MessageDialog.OK;
        var message = status.getMessage();

        switch (status.getSeverity()) {
            case IStatus.ERROR:
                kind = MessageDialog.ERROR;
                message = String.format("""
                    %s

                    %s
                    """, message, errorSuffix);
                break;
            case IStatus.WARNING:
                kind = MessageDialog.WARNING;
                break;
            case IStatus.INFO:
                kind = MessageDialog.INFORMATION;
                break;
            default:
                break;
        }

        return new DialogSpec(kind, message);
    }
}
