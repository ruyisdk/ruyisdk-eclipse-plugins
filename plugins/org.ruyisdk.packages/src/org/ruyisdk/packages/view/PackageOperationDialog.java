package org.ruyisdk.packages.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.packages.viewmodel.PackageOperationViewModel;

/**
 * Modal dialog that executes a list of install/uninstall operations sequentially, showing live
 * output. OK is disabled while operations are running; Cancel aborts remaining operations.
 *
 * <p>
 * All state is managed by the {@link PackageOperationViewModel}; this dialog simply binds its
 * widgets to the ViewModel's property-change events.
 */
public class PackageOperationDialog extends Dialog {

    private static final int ABORT_ID = IDialogConstants.CLIENT_ID + 1;

    private final PackageOperationViewModel viewModel;
    private Text liveMessage;
    private PropertyChangeListener vmListener;

    /**
     * Creates a new package-operation dialog.
     *
     * @param parentShell parent shell
     * @param viewModel the package-operation ViewModel
     */
    public PackageOperationDialog(Shell parentShell, PackageOperationViewModel viewModel) {
        super(parentShell);
        this.viewModel = viewModel;
        setShellStyle(SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
        setBlockOnOpen(true);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Package Operations");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final var container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        liveMessage = new Text(container,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        liveMessage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        vmListener = this::onViewModelChanged;
        viewModel.addPropertyChangeListener(vmListener);

        viewModel.start();

        return container;
    }

    private void onViewModelChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case PackageOperationViewModel.PROP_OUTPUT:
                if (liveMessage != null && !liveMessage.isDisposed()) {
                    liveMessage.append((String) evt.getNewValue());
                }
                break;
            case PackageOperationViewModel.PROP_RUNNING:
                final var running = (Boolean) evt.getNewValue();
                if (!running) {
                    if (getButton(OK) != null && !getButton(OK).isDisposed()) {
                        getButton(OK).setEnabled(true);
                    }
                    if (getButton(ABORT_ID) != null && !getButton(ABORT_ID).isDisposed()) {
                        getButton(ABORT_ID).setEnabled(false);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected Point getInitialSize() {
        return new Point(600, 400);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, OK, "OK", true);
        createButton(parent, ABORT_ID, "Abort", false);
        // Disable OK while operations run
        getButton(OK).setEnabled(false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == ABORT_ID) {
            viewModel.abort();
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public boolean close() {
        if (viewModel != null && vmListener != null) {
            viewModel.removePropertyChangeListener(vmListener);
        }
        return super.close();
    }
}
