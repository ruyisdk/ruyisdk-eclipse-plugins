package org.ruyisdk.packages.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A message dialog that displays its content in a scrollable, read-only text area.
 *
 * <p>
 * Use this instead of {@link org.eclipse.jface.dialogs.MessageDialog} when the message may contain
 * many lines.
 */
class ScrollableMessageDialog extends Dialog {

    private final String title;
    private final String message;
    private final int dialogType;

    /** Dialog type constant for an information dialog (single OK button). */
    static final int INFORMATION = 0;

    /** Dialog type constant for a confirmation dialog (OK / Cancel buttons). */
    static final int CONFIRM = 1;

    ScrollableMessageDialog(Shell parentShell, String title, String message, int dialogType) {
        super(parentShell);
        this.title = title;
        this.message = message;
        this.dialogType = dialogType;
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final var composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(1, false));

        final var text = new Text(composite,
                        SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        text.setText(message);

        final var gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        {
            gridData.widthHint = 500;
            gridData.heightHint = 300;
            text.setLayoutData(gridData);
        }

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (dialogType == CONFIRM) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        } else {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }
    }

    /**
     * Opens an information dialog with a scrollable message area.
     */
    static void openInformation(Shell parent, String title, String message) {
        new ScrollableMessageDialog(parent, title, message, INFORMATION).open();
    }

    /**
     * Opens a confirmation dialog with a scrollable message area.
     *
     * @return {@code true} if the user pressed OK, {@code false} otherwise.
     */
    static boolean openConfirm(Shell parent, String title, String message) {
        return new ScrollableMessageDialog(parent, title, message, CONFIRM).open() == IDialogConstants.OK_ID;
    }
}
