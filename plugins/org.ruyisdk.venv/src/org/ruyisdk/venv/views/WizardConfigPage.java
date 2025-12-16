package org.ruyisdk.venv.views;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.ruyisdk.venv.model.Emulator;
import org.ruyisdk.venv.model.Profile;
import org.ruyisdk.venv.model.Toolchain;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * Wizard page for configuring the venv profile, toolchains, and emulator options.
 */
public class WizardConfigPage extends WizardPage {

    private final VenvWizardViewModel viewModel;
    private DataBindingContext dbc;

    WizardConfigPage(VenvWizardViewModel viewModel) {
        super("configurationPage");
        this.viewModel = viewModel;
        setTitle("Venv Configuration");
        setDescription("Configure profile, toolchains and emulator options.");
    }

    @Override
    public void createControl(Composite parent) {
        dbc = new DataBindingContext();

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.addDisposeListener(e -> {
            if (dbc != null) {
                dbc.dispose();
                dbc = null;
            }
        });

        Label profileLabel = new Label(container, SWT.NONE);
        profileLabel.setText("Profile");

        TableViewer profileViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        Table profileTable = profileViewer.getTable();
        profileTable.setHeaderVisible(true);
        profileTable.setLinesVisible(true);
        profileTable.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData pd = new GridData(GridData.FILL_HORIZONTAL);
        pd.heightHint = 150;
        profileTable.setLayoutData(pd);

        TableViewerColumn pc1 = new TableViewerColumn(profileViewer, SWT.LEFT);
        pc1.getColumn().setText("Name");
        pc1.getColumn().setWidth(200);
        pc1.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Profile) element).getName();
            }
        });
        TableViewerColumn pc2 = new TableViewerColumn(profileViewer, SWT.LEFT);
        pc2.getColumn().setText("Needed Quirks");
        pc2.getColumn().setWidth(200);
        pc2.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                final String q = ((Profile) element).getQuirks();
                return q == null ? "" : q;
            }
        });

        profileViewer.setContentProvider(ArrayContentProvider.getInstance());
        profileViewer.setInput(viewModel.getProfiles());

        Label tcLabel = new Label(container, SWT.NONE);
        tcLabel.setText("Toolchains");

        Composite tcComposite = new Composite(container, SWT.NONE);
        tcComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout tcLayout = new GridLayout(2, false);
        tcLayout.marginWidth = 0;
        tcComposite.setLayout(tcLayout);

        TableViewer tcNamesViewer = new TableViewer(tcComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table tcNames = tcNamesViewer.getTable();
        tcNames.setHeaderVisible(false);
        tcNames.setLinesVisible(true);
        tcNames.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData tnData = new GridData(GridData.FILL_BOTH);
        tnData.heightHint = 120;
        tnData.widthHint = 300;
        tcNames.setLayoutData(tnData);

        TableViewer tcVersionsViewer = new TableViewer(tcComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table tcVersions = tcVersionsViewer.getTable();
        tcVersions.setHeaderVisible(false);
        tcVersions.setLinesVisible(true);
        tcVersions.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData tvData = new GridData(GridData.FILL_BOTH);
        tvData.heightHint = 120;
        tvData.widthHint = 300;
        tcVersions.setLayoutData(tvData);

        tcNamesViewer.setContentProvider(ArrayContentProvider.getInstance());
        tcNamesViewer.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Toolchain) element).getName();
            }
        });
        tcNamesViewer.setInput(viewModel.getToolchains());

        tcVersionsViewer.setContentProvider(ArrayContentProvider.getInstance());
        tcVersionsViewer.setLabelProvider(new ColumnLabelProvider());

        Composite emHeader = new Composite(container, SWT.NONE);
        GridLayout ehLayout = new GridLayout(2, false);
        ehLayout.marginWidth = 0;
        emHeader.setLayout(ehLayout);
        emHeader.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label emLabelTitle = new Label(emHeader, SWT.NONE);
        emLabelTitle.setText("Emulator");
        GridData emLabelGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        emLabelTitle.setLayoutData(emLabelGd);
        Button emulatorCheckBox = new Button(emHeader, SWT.CHECK);
        emulatorCheckBox.setText("Enable");
        GridData emChkGd = new GridData(SWT.END, SWT.CENTER, false, false);
        emulatorCheckBox.setLayoutData(emChkGd);

        Composite emComposite = new Composite(container, SWT.NONE);
        emComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout emLayout = new GridLayout(2, false);
        emLayout.marginWidth = 0;
        emComposite.setLayout(emLayout);

        TableViewer emulatorNamesViewer = new TableViewer(emComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table emulatorNames = emulatorNamesViewer.getTable();
        emulatorNames.setHeaderVisible(false);
        emulatorNames.setLinesVisible(true);
        emulatorNames.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData enData = new GridData(GridData.FILL_BOTH);
        enData.heightHint = 80;
        enData.widthHint = 300;
        emulatorNames.setLayoutData(enData);

        TableViewer emulatorVersionsViewer = new TableViewer(emComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table emulatorVersions = emulatorVersionsViewer.getTable();
        emulatorVersions.setHeaderVisible(false);
        emulatorVersions.setLinesVisible(true);
        emulatorVersions.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData evData = new GridData(GridData.FILL_BOTH);
        evData.heightHint = 80;
        evData.widthHint = 300;
        emulatorVersions.setLayoutData(evData);

        emulatorNamesViewer.setContentProvider(ArrayContentProvider.getInstance());
        emulatorNamesViewer.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Emulator) element).getName();
            }
        });
        emulatorNamesViewer.setInput(viewModel.getEmulators());

        emulatorVersionsViewer.setContentProvider(ArrayContentProvider.getInstance());
        emulatorVersionsViewer.setLabelProvider(new ColumnLabelProvider());

        Group sysrootGroup = new Group(container, SWT.NONE);
        sysrootGroup.setText("");
        GridLayout rg = new GridLayout(3, false);
        rg.marginWidth = 0;
        sysrootGroup.setLayout(rg);
        sysrootGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button rbDefault = new Button(sysrootGroup, SWT.RADIO);
        rbDefault.setText("With sysroot");
        Button rbNo = new Button(sysrootGroup, SWT.RADIO);
        rbNo.setText("Without sysroot");
        Button rbOther = new Button(sysrootGroup, SWT.RADIO);
        rbOther.setText("Use sysroot from specified package");

        var emulatorEnabledObservable = BeanProperties
                        .value(VenvWizardViewModel.class, "emulatorEnabled", Boolean.class).observe(viewModel);
        dbc.bindValue(WidgetProperties.buttonSelection().observe(emulatorCheckBox), emulatorEnabledObservable);
        dbc.bindValue(WidgetProperties.enabled().observe(emulatorNames), emulatorEnabledObservable);
        dbc.bindValue(WidgetProperties.enabled().observe(emulatorVersions), emulatorEnabledObservable);

        final SelectObservableValue<VenvWizardViewModel.SysrootOption> sysrootSelection = new SelectObservableValue<>();
        sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.DEFAULT_SYSROOT,
                        WidgetProperties.buttonSelection().observe(rbDefault));
        sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.NO_SYSROOT,
                        WidgetProperties.buttonSelection().observe(rbNo));
        sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.FROM_TOOLCHAIN,
                        WidgetProperties.buttonSelection().observe(rbOther));
        dbc.bindValue(sysrootSelection, BeanProperties
                        .value(VenvWizardViewModel.class, "sysrootOption", VenvWizardViewModel.SysrootOption.class)
                        .observe(viewModel));

        // Selection bindings: bind viewer selections to viewmodel index properties.
        final var profileSelection = ViewerProperties.singleSelection(Profile.class).observe(profileViewer);
        final UpdateValueStrategy<Profile, Integer> profileToIndex = new UpdateValueStrategy<>();
        profileToIndex.setConverter(new Converter<Profile, Integer>(Profile.class, Integer.class) {
            @Override
            public Integer convert(Profile fromObject) {
                if (fromObject == null) {
                    return Integer.valueOf(-1);
                }
                return Integer.valueOf(viewModel.getProfiles().indexOf(fromObject));
            }
        });
        final UpdateValueStrategy<Integer, Profile> indexToProfile = new UpdateValueStrategy<>();
        indexToProfile.setConverter(new Converter<Integer, Profile>(Integer.class, Profile.class) {
            @Override
            public Profile convert(Integer fromObject) {
                int idx = ((Integer) fromObject).intValue();
                return idx >= 0 && idx < viewModel.getProfiles().size() ? viewModel.getProfiles().get(idx) : null;
            }
        });
        dbc.bindValue(profileSelection, BeanProperties
                        .value(VenvWizardViewModel.class, "selectedProfileIndex", Integer.class).observe(viewModel),
                        profileToIndex, indexToProfile);

        final var toolchainSelection = ViewerProperties.singleSelection(Toolchain.class).observe(tcNamesViewer);
        final UpdateValueStrategy<Toolchain, Integer> toolchainToIndex = new UpdateValueStrategy<>();
        toolchainToIndex.setConverter(new Converter<Toolchain, Integer>(Toolchain.class, Integer.class) {
            @Override
            public Integer convert(Toolchain fromObject) {
                if (fromObject == null) {
                    return Integer.valueOf(-1);
                }
                return Integer.valueOf(viewModel.getToolchains().indexOf(fromObject));
            }
        });
        final UpdateValueStrategy<Integer, Toolchain> indexToToolchain = new UpdateValueStrategy<>();
        indexToToolchain.setConverter(new Converter<Integer, Toolchain>(Integer.class, Toolchain.class) {
            @Override
            public Toolchain convert(Integer fromObject) {
                int idx = ((Integer) fromObject).intValue();
                return idx >= 0 && idx < viewModel.getToolchains().size() ? viewModel.getToolchains().get(idx) : null;
            }
        });
        dbc.bindValue(toolchainSelection, BeanProperties
                        .value(VenvWizardViewModel.class, "selectedToolchainIndex", Integer.class).observe(viewModel),
                        toolchainToIndex, indexToToolchain);

        final var toolchainIndexObservable = BeanProperties
                        .value(VenvWizardViewModel.class, "selectedToolchainIndex", Integer.class).observe(viewModel);
        toolchainIndexObservable.addValueChangeListener(e -> {
            final int idx = viewModel.getSelectedToolchainIndex();
            if (idx >= 0 && idx < viewModel.getToolchains().size()) {
                tcVersionsViewer.setInput(viewModel.getToolchains().get(idx).getVersions());
            } else {
                tcVersionsViewer.setInput(java.util.List.of());
            }
        });
        // initialize versions viewer input
        if (viewModel.getSelectedToolchainIndex() >= 0
                        && viewModel.getSelectedToolchainIndex() < viewModel.getToolchains().size()) {
            tcVersionsViewer.setInput(
                            viewModel.getToolchains().get(viewModel.getSelectedToolchainIndex()).getVersions());
        } else {
            tcVersionsViewer.setInput(java.util.List.of());
        }

        final var toolchainVersionSelection = ViewerProperties.singleSelection(String.class).observe(tcVersionsViewer);
        final UpdateValueStrategy<String, Integer> toolchainVersionToIndex = new UpdateValueStrategy<>();
        toolchainVersionToIndex.setConverter(new Converter<String, Integer>(String.class, Integer.class) {
            @Override
            public Integer convert(String fromObject) {
                final int idx = viewModel.getSelectedToolchainIndex();
                if (idx < 0 || idx >= viewModel.getToolchains().size()) {
                    return Integer.valueOf(-1);
                }
                final var vers = viewModel.getToolchains().get(idx).getVersions();
                return Integer.valueOf(vers == null ? -1 : vers.indexOf(fromObject));
            }
        });
        final UpdateValueStrategy<Integer, String> indexToToolchainVersion = new UpdateValueStrategy<>();
        indexToToolchainVersion.setConverter(new Converter<Integer, String>(Integer.class, String.class) {
            @Override
            public String convert(Integer fromObject) {
                final int idx = viewModel.getSelectedToolchainIndex();
                if (idx < 0 || idx >= viewModel.getToolchains().size()) {
                    return null;
                }
                final var vers = viewModel.getToolchains().get(idx).getVersions();
                final int vi = ((Integer) fromObject).intValue();
                return vers != null && vi >= 0 && vi < vers.size() ? vers.get(vi) : null;
            }
        });
        dbc.bindValue(toolchainVersionSelection,
                        BeanProperties.value(VenvWizardViewModel.class, "selectedToolchainVersionIndex", Integer.class)
                                        .observe(viewModel),
                        toolchainVersionToIndex, indexToToolchainVersion);

        final var emulatorSelection = ViewerProperties.singleSelection(Emulator.class).observe(emulatorNamesViewer);
        final UpdateValueStrategy<Emulator, Integer> emulatorToIndex = new UpdateValueStrategy<>();
        emulatorToIndex.setConverter(new Converter<Emulator, Integer>(Emulator.class, Integer.class) {
            @Override
            public Integer convert(Emulator fromObject) {
                if (fromObject == null) {
                    return Integer.valueOf(-1);
                }
                return Integer.valueOf(viewModel.getEmulators().indexOf(fromObject));
            }
        });
        final UpdateValueStrategy<Integer, Emulator> indexToEmulator = new UpdateValueStrategy<>();
        indexToEmulator.setConverter(new Converter<Integer, Emulator>(Integer.class, Emulator.class) {
            @Override
            public Emulator convert(Integer fromObject) {
                int idx = ((Integer) fromObject).intValue();
                return idx >= 0 && idx < viewModel.getEmulators().size() ? viewModel.getEmulators().get(idx) : null;
            }
        });
        dbc.bindValue(emulatorSelection, BeanProperties
                        .value(VenvWizardViewModel.class, "selectedEmulatorIndex", Integer.class).observe(viewModel),
                        emulatorToIndex, indexToEmulator);

        final var emulatorIndexObservable = BeanProperties
                        .value(VenvWizardViewModel.class, "selectedEmulatorIndex", Integer.class).observe(viewModel);
        emulatorIndexObservable.addValueChangeListener(e -> {
            final int idx = viewModel.getSelectedEmulatorIndex();
            if (idx >= 0 && idx < viewModel.getEmulators().size()) {
                emulatorVersionsViewer.setInput(viewModel.getEmulators().get(idx).getVersions());
            } else {
                emulatorVersionsViewer.setInput(java.util.List.of());
            }
        });
        if (viewModel.getSelectedEmulatorIndex() >= 0
                        && viewModel.getSelectedEmulatorIndex() < viewModel.getEmulators().size()) {
            emulatorVersionsViewer
                            .setInput(viewModel.getEmulators().get(viewModel.getSelectedEmulatorIndex()).getVersions());
        } else {
            emulatorVersionsViewer.setInput(java.util.List.of());
        }

        final var emulatorVersionSelection =
                        ViewerProperties.singleSelection(String.class).observe(emulatorVersionsViewer);
        final UpdateValueStrategy<String, Integer> emulatorVersionToIndex = new UpdateValueStrategy<>();
        emulatorVersionToIndex.setConverter(new Converter<String, Integer>(String.class, Integer.class) {
            @Override
            public Integer convert(String fromObject) {
                final int idx = viewModel.getSelectedEmulatorIndex();
                if (idx < 0 || idx >= viewModel.getEmulators().size()) {
                    return Integer.valueOf(-1);
                }
                final var vers = viewModel.getEmulators().get(idx).getVersions();
                return Integer.valueOf(vers == null ? -1 : vers.indexOf(fromObject));
            }
        });
        final UpdateValueStrategy<Integer, String> indexToEmulatorVersion = new UpdateValueStrategy<>();
        indexToEmulatorVersion.setConverter(new Converter<Integer, String>(Integer.class, String.class) {
            @Override
            public String convert(Integer fromObject) {
                final int idx = viewModel.getSelectedEmulatorIndex();
                if (idx < 0 || idx >= viewModel.getEmulators().size()) {
                    return null;
                }
                final var vers = viewModel.getEmulators().get(idx).getVersions();
                final int vi = ((Integer) fromObject).intValue();
                return vers != null && vi >= 0 && vi < vers.size() ? vers.get(vi) : null;
            }
        });
        dbc.bindValue(emulatorVersionSelection,
                        BeanProperties.value(VenvWizardViewModel.class, "selectedEmulatorVersionIndex", Integer.class)
                                        .observe(viewModel),
                        emulatorVersionToIndex, indexToEmulatorVersion);

        final var completeObservable =
                        BeanProperties.value(VenvWizardViewModel.class, "configurationPageComplete", Boolean.class)
                                        .observe(viewModel);
        completeObservable.addValueChangeListener(e -> updatePageComplete());

        setControl(container);
        updatePageComplete();
    }

    private void updatePageComplete() {
        setPageComplete(viewModel.isConfigurationPageComplete());
        if (getWizard() != null && getWizard().getContainer() != null) {
            getWizard().getContainer().updateButtons();
        }
    }
}
