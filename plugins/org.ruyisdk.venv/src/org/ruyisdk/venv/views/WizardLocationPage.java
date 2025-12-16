package org.ruyisdk.venv.views;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * Wizard page for selecting a venv location and name.
 */
public class WizardLocationPage extends org.eclipse.jface.wizard.WizardPage {

    private final VenvWizardViewModel viewModel;
    private Combo pathCombo;
    private ComboViewer pathComboViewer;
    private Text venvNameText;
    private Text summaryText;
    private DataBindingContext dbc;


    WizardLocationPage(VenvWizardViewModel vm) {
        super("locationPage");
        this.viewModel = vm;
        setTitle("Venv Location");
        setDescription("Review summary and select the venv location.");
    }


    @Override
    public void createControl(Composite parent) {
        dbc = new DataBindingContext();

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));
        container.addDisposeListener(e -> {
            if (dbc != null) {
                dbc.dispose();
                dbc = null;
            }
        });

        Label summaryLabel = new Label(container, SWT.NONE);
        summaryLabel.setText("Summary:");
        GridData labelGd = new GridData(GridData.FILL_HORIZONTAL);
        labelGd.horizontalSpan = 3;
        summaryLabel.setLayoutData(labelGd);

        summaryText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData sd = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        sd.horizontalSpan = 3;
        summaryText.setLayoutData(sd);
        summaryText.setEditable(false);
        summaryText.setText(viewModel.getSummaryText());

        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Venv Name:");
        venvNameText = new Text(container, SWT.BORDER);
        venvNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(container, SWT.NONE);

        Label locationLabel = new Label(container, SWT.NONE);
        locationLabel.setText("Venv Path:");
        pathCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN);
        pathCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        pathCombo.setText("");
        pathComboViewer = new ComboViewer(pathCombo);
        ViewerSupport.bind(pathComboViewer, viewModel.getProjectRootPaths(), Properties.selfValue(String.class));

        final var pathObservable = WidgetProperties.comboSelection().observe(pathCombo);
        final var nameObservable = WidgetProperties.text(SWT.Modify).observe(venvNameText);

        dbc.bindValue(pathObservable, BeanProperties.value(VenvWizardViewModel.class, "venvLocation", String.class)
                        .observe(viewModel));
        dbc.bindValue(nameObservable,
                        BeanProperties.value(VenvWizardViewModel.class, "venvName", String.class).observe(viewModel));
        dbc.bindValue(WidgetProperties.text().observe(summaryText), BeanProperties
                        .value(VenvWizardViewModel.class, "summaryText", String.class).observe(viewModel));

        final var completeObservable = new ComputedValue<Boolean>() {
            @Override
            protected Boolean calculate() {
                final String path = pathObservable.getValue();
                final String name = nameObservable.getValue();
                return path != null && !path.trim().isEmpty() && name != null && !name.trim().isEmpty();
            }
        };
        completeObservable.addValueChangeListener(e -> {
            setPageComplete(Boolean.TRUE.equals(completeObservable.getValue()));
            if (getWizard() != null && getWizard().getContainer() != null) {
                getWizard().getContainer().updateButtons();
            }
        });

        org.eclipse.swt.widgets.Button browse = new org.eclipse.swt.widgets.Button(container, SWT.PUSH);
        browse.setText("Browse...");
        browse.addListener(SWT.Selection, e -> {
            org.eclipse.swt.widgets.DirectoryDialog dd = new org.eclipse.swt.widgets.DirectoryDialog(getShell());
            String sel = dd.open();
            if (sel != null) {
                pathCombo.setText(sel);
            }
        });

        setControl(container);
        setPageComplete(Boolean.TRUE.equals(completeObservable.getValue()));
    }
}
