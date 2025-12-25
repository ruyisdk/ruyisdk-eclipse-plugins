package org.ruyisdk.venv.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardDialog;
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
    private DataBindingContext dbc;

    private Composite container;
    private Composite header;
    private Composite tableComposite;
    private Composite buttonBar;

    private Button absPathCheckBox;
    private TableViewer tableViewer;
    private Button applyButton;
    private Button deleteButton;
    private Button newButton;

    @Override
    public void createPartControl(Composite parent) {
        final var activator = Activator.getDefault();
        venvListViewModel = new VenvListViewModel(activator.getService(), activator.getConfigService());

        createLayouts(parent);
        addControls();
        registerEvents();

        // initial data load
        venvListViewModel.onRefreshVenvListAsync();
    }

    private void createLayouts(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        header = new Composite(container, SWT.NONE);
        header.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        header.setLayout(new GridLayout(2, false));

        tableComposite = new Composite(container, SWT.NONE);
        tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        buttonBar = new Composite(container, SWT.NONE);
        buttonBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            var gridLayout = new GridLayout(3, false);
            gridLayout.marginWidth = 0;
            buttonBar.setLayout(gridLayout);
        }
    }

    private void addControls() {
        final var headerLabel = new Label(header, SWT.NONE);
        headerLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        headerLabel.setText("Detected virtual environments in open projects:");

        absPathCheckBox = new Button(header, SWT.CHECK);
        absPathCheckBox.setText("Show absolute path");
        absPathCheckBox.setEnabled(false);
        absPathCheckBox.setSelection(true);

        // table
        {
            tableViewer = new TableViewer(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
            final var tableColumnLayout = new TableColumnLayout();
            {
                final var column = new TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Profile");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(15, 120));
            }
            {
                final var column = new TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Toolchain Prefix");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(20, 180));
            }
            {
                final var column = new TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Sysroot");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(30, 250));
            }
            {
                final var column = new TableViewerColumn(tableViewer, SWT.LEFT);
                column.getColumn().setText("Project Path");
                tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(35, 300));
            }
            tableComposite.setLayout(tableColumnLayout);

            tableViewer.getTable().setHeaderVisible(true);
            tableViewer.getTable().setLinesVisible(true);

            final var contentProvider = new ObservableListContentProvider<Venv>();
            tableViewer.setContentProvider(contentProvider);
            tableViewer.setLabelProvider(new ObservableMapLabelProvider(
                            Properties.observeEach(contentProvider.getKnownElements(), BeanProperties.values(Venv.class,
                                            "profile", "toolchainPrefix", "sysroot", "projectPath"))));

            tableViewer.setInput(venvListViewModel.getVenvList());
        }

        applyButton = new Button(buttonBar, SWT.PUSH);
        applyButton.setText("Apply to Project");
        applyButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        deleteButton = new Button(buttonBar, SWT.PUSH);
        deleteButton.setText("Delete...");
        deleteButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        newButton = new Button(buttonBar, SWT.PUSH);
        newButton.setText("New virtual environment...");
        newButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
    }

    private void registerEvents() {
        dbc = new DataBindingContext();

        dbc.bindList(ViewerProperties.multipleSelection().observe(tableViewer), venvListViewModel.getSelectedVenvs());
        dbc.bindValue(WidgetProperties.enabled().observe(deleteButton), BeanProperties
                        .value(VenvListViewModel.class, "canDelete", Boolean.class).observe(venvListViewModel));
        dbc.bindValue(WidgetProperties.enabled().observe(applyButton), BeanProperties
                        .value(VenvListViewModel.class, "canApply", Boolean.class).observe(venvListViewModel));

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                venvListViewModel.onApplySelectedVenvConfig(result -> {
                    container.getDisplay().asyncExec(() -> {
                        MessageDialog.openInformation(container.getShell(), "Apply Configuration", result.getMessage());
                    });
                });
            }
        });

        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final var venvPaths = venvListViewModel.getSelectedVenvDirectoryPaths();
                if (venvPaths.isEmpty()) {
                    return;
                }

                final String message;
                if (venvPaths.size() == 1) {
                    message = "This will delete the whole directory of the virtual environment:\n\n" + venvPaths.get(0)
                                    + "\n\nContinue?";
                } else {
                    message = "This will delete the whole directories of the selected virtual environments:\n\n"
                                    + String.join("\n", venvPaths) + "\n\nContinue?";
                }

                final var confirmDeletion =
                                MessageDialog.openConfirm(container.getShell(), "Delete virtual environment", message);
                if (!confirmDeletion) {
                    return;
                }

                venvListViewModel.onDeleteSelectedVenvDirectories(err -> {
                    if (err != null) {
                        MessageDialog.openError(container.getShell(), "Delete virtual environment failed",
                                        "Failed to delete:\n\n" + err.getMessage());
                    }
                });
            }
        });

        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final var wizardViewModel = new VenvWizardViewModel(Activator.getDefault().getService());
                wizardViewModel.setProjectRootPaths(getOpenProjectRootPaths());
                final var dialog = new WizardDialog(container.getShell(), new VenvWizard(wizardViewModel));
                if (dialog.open() == WizardDialog.OK) {
                    venvListViewModel.onRefreshVenvListAsync();
                }
            }
        });
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


    private static List<String> getOpenProjectRootPaths() {
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
