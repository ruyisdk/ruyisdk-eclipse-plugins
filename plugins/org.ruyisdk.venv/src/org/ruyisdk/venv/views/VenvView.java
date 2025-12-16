package org.ruyisdk.venv.views;

import java.util.ArrayList;
import java.util.Collections;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.ruyisdk.venv.Activator;
import org.ruyisdk.venv.model.Venv;
import org.ruyisdk.venv.viewmodel.VenvListViewModel;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * View showing detected virtual environments and actions to manage them.
 */
public class VenvView extends ViewPart {

    public static final String ID = "org.ruyisdk.venv.view";

    private VenvListViewModel venvListViewModel;
    private TableViewer tableViewer;
    private DataBindingContext dbc;

    @Override
    public void createPartControl(Composite parent) {
        venvListViewModel = new VenvListViewModel(Activator.getDefault().getService());
        dbc = new DataBindingContext();

        parent.setLayout(new GridLayout());

        // Header: label + disabled checkbox (right aligned)
        Composite header = new Composite(parent, SWT.NONE);
        header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        header.setLayout(new GridLayout(2, false));
        Label headerLabel = new Label(header, SWT.NONE);
        headerLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        headerLabel.setText("Detected virtual environments in open projects:");
        Button absPathChk = new Button(header, SWT.CHECK);
        absPathChk.setText("Show absolute path");
        absPathChk.setEnabled(false);
        absPathChk.setSelection(true);

        // table
        {
            var tableComposite = new Composite(parent, SWT.NONE);
            tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

            tableViewer = new TableViewer(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
            final var tableColumnLayout = new TableColumnLayout();
            {
                final var column = new org.eclipse.jface.viewers.TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Profile");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(20, 150));
            }
            {
                final var column = new org.eclipse.jface.viewers.TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Sysroot");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(40, 300));
            }
            {
                final var column = new org.eclipse.jface.viewers.TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Project Path");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(60, 500));
            }
            tableComposite.setLayout(tableColumnLayout);

            tableViewer.getTable().setHeaderVisible(true);
            tableViewer.getTable().setLinesVisible(true);

            final var contentProvider = new ObservableListContentProvider<Venv>();
            tableViewer.setContentProvider(contentProvider);
            tableViewer.setLabelProvider(
                            new ObservableMapLabelProvider(Properties.observeEach(contentProvider.getKnownElements(),
                                            BeanProperties.values(Venv.class, "profile", "sysroot", "projectPath"))));

            tableViewer.setInput(venvListViewModel.getVenvList());
        }

        // Button row: toggle, delete (left), new (right)
        Composite buttonBar = new Composite(parent, SWT.NONE);
        buttonBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout bl = new GridLayout(3, false);
        bl.marginWidth = 0;
        buttonBar.setLayout(bl);


        // Activate/Deactivate button (left)
        final var toggleButton = new Button(buttonBar, SWT.PUSH);
        toggleButton.setText("Toggle activation");
        toggleButton.setEnabled(false);
        GridData gdToggle = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        toggleButton.setLayoutData(gdToggle);

        // Delete button (left)
        final var deleteButton = new Button(buttonBar, SWT.PUSH);
        deleteButton.setText("Delete...");
        GridData gdDelete = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        deleteButton.setLayoutData(gdDelete);

        final var newButton = new org.eclipse.swt.widgets.Button(buttonBar, SWT.PUSH);
        newButton.setText("New virtual environment...");
        GridData gdNew = new GridData(SWT.END, SWT.CENTER, true, false);
        newButton.setLayoutData(gdNew);

        // Bind table selection -> viewmodel selection list.
        dbc.bindList(ViewerProperties.multipleSelection().observe(tableViewer), venvListViewModel.getSelectedVenvs());
        // Bind Delete button enabled state to viewmodel validation.
        dbc.bindValue(WidgetProperties.enabled().observe(deleteButton), BeanProperties
                        .value(VenvListViewModel.class, "canDelete", Boolean.class).observe(venvListViewModel));

        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final var wizardVm = new VenvWizardViewModel(Activator.getDefault().getService());
                wizardVm.setProjectRootPaths(getOpenProjectRootPaths());
                final var dialog =
                                new org.eclipse.jface.wizard.WizardDialog(parent.getShell(), new VenvWizard(wizardVm));
                boolean ok = dialog.open() == org.eclipse.jface.wizard.WizardDialog.OK;
                if (ok) {
                    venvListViewModel.onRefreshVenvList();
                }
            }
        });

        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final var paths = venvListViewModel.getSelectedVenvDirectoryPaths();
                if (paths.isEmpty()) {
                    return;
                }

                final String message;
                if (paths.size() == 1) {
                    message = "This will delete the whole directory of the virtual environment:\n\n" + paths.get(0)
                                    + "\n\nContinue?";
                } else {
                    message = "This will delete the whole directories of the selected virtual environments:\n\n"
                                    + String.join("\n", paths) + "\n\nContinue?";
                }

                final boolean ok = MessageDialog.openConfirm(parent.getShell(), "Delete virtual environment", message);
                if (!ok) {
                    return;
                }

                venvListViewModel.onDeleteSelectedVenvDirectories(err -> {
                    if (err != null) {
                        MessageDialog.openError(parent.getShell(), "Delete virtual environment failed",
                                        "Failed to delete:\n\n" + err.getMessage());
                    }
                });
            }
        });

        venvListViewModel.onRefreshVenvList();
    }


    @Override
    public void dispose() {
        if (dbc != null) {
            dbc.dispose();
            dbc = null;
        }
        super.dispose();
    }


    @Override
    public void setFocus() {}


    private static java.util.List<String> getOpenProjectRootPaths() {
        final var root = ResourcesPlugin.getWorkspace().getRoot();
        final var projects = root.getProjects();
        final var result = new ArrayList<String>();
        for (final var project : projects) {
            if (project == null || !project.isOpen()) {
                continue;
            }
            final var location = project.getLocation();
            if (location == null) {
                continue;
            }
            result.add(location.toOSString());
        }
        Collections.sort(result);
        return result;
    }
}
