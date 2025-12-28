package org.ruyisdk.venv.views;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * Wizard page for selecting a venv location and name.
 */
public class WizardLocationPage extends WizardPage {

    private final VenvWizardViewModel viewModel;
    private DataBindingContext dbc;

    private Composite container;

    private Text summaryText;
    private Text venvNameText;
    private Combo venvPathCombo;
    private ComboViewer venvPathComboViewer;
    private Button browseButton;


    WizardLocationPage(VenvWizardViewModel viewModel) {
        super("locationPage");
        this.viewModel = viewModel;
        setTitle("Venv Location");
        setDescription("Review summary and select the venv location.");
    }


    @Override
    public void createControl(Composite parent) {
        createLayouts(parent);
        addControls();
        registerEvents();
    }

    private void createLayouts(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        setControl(container);
    }

    private void addControls() {
        final var summaryLabel = new Label(container, SWT.NONE);
        {
            final var gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.horizontalSpan = 3;
            summaryLabel.setLayoutData(gridData);
        }
        summaryLabel.setText("Summary:");

        summaryText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        {
            final var gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
            gridData.horizontalSpan = 3;
            summaryText.setLayoutData(gridData);
        }
        summaryText.setText(viewModel.getSummaryText());
        summaryText.setEditable(false);

        final var nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Venv Name:");

        venvNameText = new Text(container, SWT.BORDER);
        {
            var gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.horizontalSpan = 2;
            venvNameText.setLayoutData(gridData);
        }

        final var locationLabel = new Label(container, SWT.NONE);
        locationLabel.setText("Venv Path:");
        venvPathCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN);
        venvPathCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        venvPathCombo.setText("");
        venvPathComboViewer = new ComboViewer(venvPathCombo);

        browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("Browse...");
    }

    private void registerEvents() {
        dbc = new DataBindingContext();

        container.addDisposeListener(e -> {
            if (dbc != null) {
                dbc.dispose();
                dbc = null;
            }
        });

        final var nameObservable = WidgetProperties.text(SWT.Modify).observe(venvNameText);
        final var pathObservable = WidgetProperties.comboSelection().observe(venvPathCombo);

        dbc.bindValue(pathObservable, BeanProperties.value(VenvWizardViewModel.class, "venvLocation", String.class)
                        .observe(viewModel));
        dbc.bindValue(nameObservable,
                        BeanProperties.value(VenvWizardViewModel.class, "venvName", String.class).observe(viewModel));
        dbc.bindValue(WidgetProperties.text().observe(summaryText), BeanProperties
                        .value(VenvWizardViewModel.class, "summaryText", String.class).observe(viewModel));

        ViewerSupport.bind(venvPathComboViewer, viewModel.getProjectRootPaths(), Properties.selfValue(String.class));

        browseButton.addListener(SWT.Selection, e -> {
            String selectedPath = new DirectoryDialog(getShell()).open();
            if (selectedPath != null) {
                venvPathCombo.setText(selectedPath);
            }
        });

        final var completeObservable = new ComputedValue<Boolean>() {
            @Override
            protected Boolean calculate() {
                final var path = pathObservable.getValue();
                final var name = nameObservable.getValue();
                return path != null && !path.isBlank() && name != null && !name.isBlank();
            }
        };
        completeObservable.addValueChangeListener(e -> {
            setPageComplete(Boolean.TRUE.equals(completeObservable.getValue()));
            if (getWizard() != null && getWizard().getContainer() != null) {
                getWizard().getContainer().updateButtons();
            }
        });
        setPageComplete(Boolean.TRUE.equals(completeObservable.getValue()));
    }
}
