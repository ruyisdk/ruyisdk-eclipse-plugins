package org.ruyisdk.packages.view;

import java.util.List;
import java.util.function.BiConsumer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.packages.viewmodel.DeviceSelectionViewModel;
import org.ruyisdk.ruyi.model.DeviceEntityInfo;

/**
 * Dialog for selecting a device using a sortable table.
 *
 * <p>
 * All data is read from the {@link DeviceSelectionViewModel}. When the user presses OK the
 * {@code onConfirm} callback is invoked with the selected device and a {@link Runnable} that the
 * caller must eventually invoke to close the dialog. this allows the caller to perform an
 * asynchronous reload before the dialog disappears.
 */
public class DeviceSelectionDialog extends Dialog {

    private static final int CLEAR_ID = IDialogConstants.CLIENT_ID + 1;

    private final DeviceSelectionViewModel viewModel;
    private final BiConsumer<DeviceEntityInfo, Runnable> onConfirm;

    private TableViewer tableViewer;
    private Text statusText;
    private int sortColumnIndex = 0;
    private int sortDirection = SWT.UP;

    /**
     * Creates a new device-selection dialog.
     *
     * @param parentShell parent shell
     * @param viewModel the device-selection ViewModel
     * @param onConfirm callback invoked on OK: {@code (selectedDevice, onDone)}. The caller must
     *        invoke {@code onDone} (on the UI thread) to close the dialog.
     */
    public DeviceSelectionDialog(Shell parentShell, DeviceSelectionViewModel viewModel,
            BiConsumer<DeviceEntityInfo, Runnable> onConfirm) {
        super(parentShell);
        this.viewModel = viewModel;
        this.onConfirm = onConfirm;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Select Device");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final var container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        tableViewer = new TableViewer(container,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        final var table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Column: Name
        final var nameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().setWidth(250);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DeviceEntityInfo) element).getLabel();
            }
        });

        // Column: ID
        final var idColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        idColumn.getColumn().setText("ID");
        idColumn.getColumn().setWidth(250);
        idColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DeviceEntityInfo) element).getEntityId();
            }
        });

        // Column: Variants
        final var variantsColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        variantsColumn.getColumn().setText("Variants");
        // Any positive number works without effect;
        // platform fill the remaining width automatically.
        variantsColumn.getColumn().setWidth(100);
        variantsColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                final var device = (DeviceEntityInfo) element;
                return String.join(", ", device.getVariantNames());
            }
        });

        // Content provider
        tableViewer.setContentProvider(new IStructuredContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return ((List<?>) inputElement).toArray();
            }

            @Override
            public void dispose() {
                // nothing to dispose
            }

            @Override
            public void inputChanged(Viewer v, Object oldInput, Object newInput) {
                // no action needed
            }
        });

        // Sorting support
        tableViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer v, Object e1, Object e2) {
                final var d1 = (DeviceEntityInfo) e1;
                final var d2 = (DeviceEntityInfo) e2;
                int result;
                switch (sortColumnIndex) {
                    case 1:
                        result = d1.getEntityId().compareToIgnoreCase(d2.getEntityId());
                        break;
                    case 2:
                        result = Integer.compare(d1.getRelatedRefs().size(),
                                d2.getRelatedRefs().size());
                        break;
                    default:
                        result = d1.getLabel().compareToIgnoreCase(d2.getLabel());
                        break;
                }
                return sortDirection == SWT.UP ? result : -result;
            }
        });

        // Click column headers to sort
        final var columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            final var colIdx = i;
            columns[i].addListener(SWT.Selection, e -> {
                if (sortColumnIndex == colIdx) {
                    sortDirection = (sortDirection == SWT.UP) ? SWT.DOWN : SWT.UP;
                } else {
                    sortColumnIndex = colIdx;
                    sortDirection = SWT.UP;
                }
                table.setSortColumn(columns[colIdx]);
                table.setSortDirection(sortDirection);
                tableViewer.refresh();
            });
        }

        // Set initial sort indicator
        table.setSortColumn(columns[0]);
        table.setSortDirection(SWT.UP);

        tableViewer.setInput(viewModel.getDevices());

        // Status area for loading/error feedback
        statusText = new Text(container,
                SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        {
            final var gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
            gridData.heightHint = 50;
            statusText.setLayoutData(gridData);
        }
        statusText.setText(viewModel.getStatusText());

        // Selection tracking
        tableViewer.addSelectionChangedListener(event -> {
            final var selection = (StructuredSelection) event.getSelection();
            viewModel.setSelectedDevice((DeviceEntityInfo) selection.getFirstElement());
            getButton(CLEAR_ID).setEnabled(viewModel.getSelectedDevice() != null);
            statusText.setText(viewModel.getStatusText());
        });

        return container;
    }

    /**
     * Reload packages BEFORE closing the dialog, otherwise users may click other controls while the
     * package list is loading.
     */
    @Override
    protected void okPressed() {
        tableViewer.getTable().setEnabled(false);
        getButton(OK).setEnabled(false);
        getButton(CLEAR_ID).setEnabled(false);
        getButton(CANCEL).setEnabled(false);

        onConfirm.accept(viewModel.getSelectedDevice(), () -> {
            if (getShell() != null && !getShell().isDisposed()) {
                super.okPressed();
            }
        });
    }

    @Override
    protected Point getInitialSize() {
        return new Point(650, 500);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, CLEAR_ID, "Clear", false);
        createButton(parent, CANCEL, "Cancel", false);
        createButton(parent, OK, "OK", true);

        // Enable OK only if devices are already fetched.
        // Pressing OK without selected device is allowed and means "clear selection".
        getButton(OK).setEnabled(viewModel.hasDevices());
        getButton(CLEAR_ID).setEnabled(viewModel.getSelectedDevice() != null);

        // Pre-select the currently chosen device, if any
        final var current = viewModel.getSelectedDevice();
        if (current != null) {
            for (final var device : viewModel.getDevices()) {
                if (device.getEntityId().equals(current.getEntityId())) {
                    tableViewer.setSelection(new StructuredSelection(device), true);
                    break;
                }
            }
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == CLEAR_ID) {
            tableViewer.setSelection(StructuredSelection.EMPTY);
        } else {
            super.buttonPressed(buttonId);
        }
    }
}
