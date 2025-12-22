package org.ruyisdk.venv.views;

import java.util.List;
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

    private Composite container;
    private Composite profileComposite;
    private Composite toolchainComposite;
    private Composite emulatorHeader;
    private Composite emulatorComposite;
    private Group sysrootGroup;

    private TableViewer profileTableViewer;
    private TableViewer toolchainNamesViewer;
    private TableViewer toolchainVersionsViewer;
    private Button emulatorCheckBox;
    private Table emulatorNames;
    private TableViewer emulatorNamesViewer;
    private Table emulatorVersions;
    private TableViewer emulatorVersionsViewer;
    private Button sysrootDefaultRadio;
    private Button sysrootNoneRadio;
    private Button sysrootForeignRadio;

    WizardConfigPage(VenvWizardViewModel viewModel) {
        super("configurationPage");
        this.viewModel = viewModel;
        setTitle("Venv Configuration");
        setDescription("Configure profile, toolchains and emulator options.");
    }

    @Override
    public void createControl(Composite parent) {
        createLayouts(parent);
        addControls();
        registerEvents();

        // initialize states
        updatePageComplete();
    }

    private void createLayouts(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        profileComposite = new Composite(container, SWT.NONE);
        profileComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            var gridLayout = new GridLayout(1, false);
            gridLayout.marginWidth = 0;
            profileComposite.setLayout(gridLayout);
        }

        toolchainComposite = new Composite(container, SWT.NONE);
        toolchainComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            var gridLayout = new GridLayout(2, false);
            gridLayout.marginWidth = 0;
            toolchainComposite.setLayout(gridLayout);
        }

        emulatorHeader = new Composite(container, SWT.NONE);
        emulatorHeader.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            var gridLayout = new GridLayout(2, false);
            gridLayout.marginWidth = 0;
            emulatorHeader.setLayout(gridLayout);
        }

        emulatorComposite = new Composite(container, SWT.NONE);
        emulatorComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            var gridLayout = new GridLayout(2, false);
            gridLayout.marginWidth = 0;
            emulatorComposite.setLayout(gridLayout);
        }

        sysrootGroup = new Group(container, SWT.NONE);
        sysrootGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sysrootGroup.setText("");
        {
            var gridLayout = new GridLayout(3, false);
            gridLayout.marginWidth = 0;
            sysrootGroup.setLayout(gridLayout);
        }

        setControl(container);
    }

    private void addControls() {
        // profiles
        {
            final var profileLabel = new Label(profileComposite, SWT.NONE);
            profileLabel.setText("Profiles");

            profileTableViewer = new TableViewer(profileComposite, SWT.BORDER | SWT.FULL_SELECTION);
            final var profileTable = profileTableViewer.getTable();
            {
                final var gridData = new GridData(GridData.FILL_HORIZONTAL);
                gridData.heightHint = 150;
                profileTable.setLayoutData(gridData);
            }
            profileTable.setHeaderVisible(true);
            profileTable.setLinesVisible(true);

            {
                final var column = new TableViewerColumn(profileTableViewer, SWT.LEFT);
                column.getColumn().setText("Name");
                column.getColumn().setWidth(200);
                column.setLabelProvider(new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        return ((Profile) element).getName();
                    }
                });
            }
            {
                final var column = new TableViewerColumn(profileTableViewer, SWT.LEFT);
                column.getColumn().setText("Needed Quirks");
                column.getColumn().setWidth(400);
                column.setLabelProvider(new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        final var q = ((Profile) element).getQuirks();
                        return q == null ? "" : q;
                    }
                });
            }

            profileTableViewer.setContentProvider(ArrayContentProvider.getInstance());
            profileTableViewer.setInput(viewModel.getProfiles());
        }

        // toolchains
        {
            final var tcLabel = new Label(toolchainComposite, SWT.NONE);
            {
                final var gridData = new GridData();
                gridData.horizontalSpan = 2;
                tcLabel.setLayoutData(gridData);
            }
            tcLabel.setText("Toolchains");

            toolchainNamesViewer = new TableViewer(toolchainComposite, SWT.BORDER | SWT.FULL_SELECTION);
            final var tcNames = toolchainNamesViewer.getTable();
            {
                final var gridData = new GridData(GridData.FILL_BOTH);
                gridData.heightHint = 120;
                gridData.widthHint = 300;
                tcNames.setLayoutData(gridData);
            }
            tcNames.setHeaderVisible(false);
            tcNames.setLinesVisible(true);

            toolchainVersionsViewer = new TableViewer(toolchainComposite, SWT.BORDER | SWT.FULL_SELECTION);
            final var tcVersions = toolchainVersionsViewer.getTable();
            {
                final var gridData = new GridData(GridData.FILL_BOTH);
                gridData.heightHint = 120;
                gridData.widthHint = 300;
                tcVersions.setLayoutData(gridData);
            }
            tcVersions.setHeaderVisible(false);
            tcVersions.setLinesVisible(true);

            toolchainNamesViewer.setContentProvider(ArrayContentProvider.getInstance());
            toolchainNamesViewer.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((Toolchain) element).getName();
                }
            });
            toolchainNamesViewer.setInput(viewModel.getToolchains());

            toolchainVersionsViewer.setContentProvider(ArrayContentProvider.getInstance());
            toolchainVersionsViewer.setLabelProvider(new ColumnLabelProvider());
        }

        // emulators
        {
            final var emLabelTitle = new Label(emulatorHeader, SWT.NONE);
            emLabelTitle.setText("Emulators");
            final var emLabelGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
            emLabelTitle.setLayoutData(emLabelGd);
            emulatorCheckBox = new Button(emulatorHeader, SWT.CHECK);
            emulatorCheckBox.setText("Enable");
            emulatorCheckBox.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

            emulatorNamesViewer = new TableViewer(emulatorComposite, SWT.BORDER | SWT.FULL_SELECTION);
            emulatorNames = emulatorNamesViewer.getTable();
            emulatorNames.setHeaderVisible(false);
            emulatorNames.setLinesVisible(true);
            final var enData = new GridData(GridData.FILL_BOTH);
            enData.heightHint = 80;
            enData.widthHint = 300;
            emulatorNames.setLayoutData(enData);

            emulatorVersionsViewer = new TableViewer(emulatorComposite, SWT.BORDER | SWT.FULL_SELECTION);
            emulatorVersions = emulatorVersionsViewer.getTable();
            emulatorVersions.setHeaderVisible(false);
            emulatorVersions.setLinesVisible(true);
            final var evData = new GridData(GridData.FILL_BOTH);
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
        }

        // sysroot
        {
            sysrootDefaultRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootDefaultRadio.setText("With sysroot");
            sysrootNoneRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootNoneRadio.setText("Without sysroot");
            sysrootForeignRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootForeignRadio.setText("Use sysroot from specified package");
            sysrootForeignRadio.setEnabled(false); // TODO: implement this option later
        }
    }

    private void registerEvents() {
        dbc = new DataBindingContext();

        container.addDisposeListener(e -> {
            if (dbc != null) {
                dbc.dispose();
                dbc = null;
            }
        });

        // profiles
        {
            final var profileSelection = ViewerProperties.singleSelection(Profile.class).observe(profileTableViewer);
            final var profileIndexObservable = BeanProperties
                            .value(VenvWizardViewModel.class, "selectedProfileIndex", Integer.class).observe(viewModel);
            final var profileToIndex = new UpdateValueStrategy<Profile, Integer>();
            profileToIndex.setConverter(new Converter<Profile, Integer>(Profile.class, Integer.class) {
                @Override
                public Integer convert(Profile fromObject) {
                    if (fromObject == null) {
                        return Integer.valueOf(-1);
                    }
                    return Integer.valueOf(viewModel.getProfiles().indexOf(fromObject));
                }
            });
            final var indexToProfile = new UpdateValueStrategy<Integer, Profile>();
            indexToProfile.setConverter(new Converter<Integer, Profile>(Integer.class, Profile.class) {
                @Override
                public Profile convert(Integer fromObject) {
                    final var idx = ((Integer) fromObject).intValue();
                    return idx >= 0 && idx < viewModel.getProfiles().size() ? viewModel.getProfiles().get(idx) : null;
                }
            });
            dbc.bindValue(profileSelection, profileIndexObservable, profileToIndex, indexToProfile);
        }

        // toolchain names
        {
            final var toolchainSelection =
                            ViewerProperties.singleSelection(Toolchain.class).observe(toolchainNamesViewer);
            final var toolchainIndexObservable =
                            BeanProperties.value(VenvWizardViewModel.class, "selectedToolchainIndex", Integer.class)
                                            .observe(viewModel);
            final var toolchainToIndex = new UpdateValueStrategy<Toolchain, Integer>();
            toolchainToIndex.setConverter(new Converter<Toolchain, Integer>(Toolchain.class, Integer.class) {
                @Override
                public Integer convert(Toolchain fromObject) {
                    if (fromObject == null) {
                        return Integer.valueOf(-1);
                    }
                    return Integer.valueOf(viewModel.getToolchains().indexOf(fromObject));
                }
            });
            final var indexToToolchain = new UpdateValueStrategy<Integer, Toolchain>();
            indexToToolchain.setConverter(new Converter<Integer, Toolchain>(Integer.class, Toolchain.class) {
                @Override
                public Toolchain convert(Integer fromObject) {
                    final var idx = ((Integer) fromObject).intValue();
                    if (idx >= 0 && idx < viewModel.getToolchains().size()) {
                        return viewModel.getToolchains().get(idx);
                    }
                    return null;
                }
            });
            dbc.bindValue(toolchainSelection, toolchainIndexObservable, toolchainToIndex, indexToToolchain);

            toolchainIndexObservable.addValueChangeListener(e -> {
                final var idx = viewModel.getSelectedToolchainIndex();
                if (idx >= 0 && idx < viewModel.getToolchains().size()) {
                    toolchainVersionsViewer.setInput(viewModel.getToolchains().get(idx).getVersions());
                } else {
                    toolchainVersionsViewer.setInput(List.of());
                }
            });
        }

        // toolchain versions
        {
            final var toolchainVersionSelection =
                            ViewerProperties.singleSelection(String.class).observe(toolchainVersionsViewer);
            final var toolchainVersionIndexObservable = BeanProperties
                            .value(VenvWizardViewModel.class, "selectedToolchainVersionIndex", Integer.class)
                            .observe(viewModel);
            final var toolchainVersionToIndex = new UpdateValueStrategy<String, Integer>();
            toolchainVersionToIndex.setConverter(new Converter<String, Integer>(String.class, Integer.class) {
                @Override
                public Integer convert(String fromObject) {
                    final var idx = viewModel.getSelectedToolchainIndex();
                    if (idx < 0 || idx >= viewModel.getToolchains().size()) {
                        return Integer.valueOf(-1);
                    }
                    final var vers = viewModel.getToolchains().get(idx).getVersions();
                    return Integer.valueOf(vers == null ? -1 : vers.indexOf(fromObject));
                }
            });
            final var indexToToolchainVersion = new UpdateValueStrategy<Integer, String>();
            indexToToolchainVersion.setConverter(new Converter<Integer, String>(Integer.class, String.class) {
                @Override
                public String convert(Integer fromObject) {
                    final var idx = viewModel.getSelectedToolchainIndex();
                    if (idx < 0 || idx >= viewModel.getToolchains().size()) {
                        return null;
                    }
                    final var vers = viewModel.getToolchains().get(idx).getVersions();
                    final var verIdx = ((Integer) fromObject).intValue();
                    return vers != null && verIdx >= 0 && verIdx < vers.size() ? vers.get(verIdx) : null;
                }
            });
            dbc.bindValue(toolchainVersionSelection, toolchainVersionIndexObservable, toolchainVersionToIndex,
                            indexToToolchainVersion);
        }

        // emulator enablement
        {
            final var emulatorEnabledObservable = BeanProperties
                            .value(VenvWizardViewModel.class, "emulatorEnabled", Boolean.class).observe(viewModel);
            dbc.bindValue(WidgetProperties.buttonSelection().observe(emulatorCheckBox), emulatorEnabledObservable);
            dbc.bindValue(WidgetProperties.enabled().observe(emulatorNames), emulatorEnabledObservable);
            dbc.bindValue(WidgetProperties.enabled().observe(emulatorVersions), emulatorEnabledObservable);
        }

        // emulator names
        {
            final var emulatorSelection = ViewerProperties.singleSelection(Emulator.class).observe(emulatorNamesViewer);
            final var emulatorIndexObservable =
                            BeanProperties.value(VenvWizardViewModel.class, "selectedEmulatorIndex", Integer.class)
                                            .observe(viewModel);
            final var emulatorToIndex = new UpdateValueStrategy<Emulator, Integer>();
            emulatorToIndex.setConverter(new Converter<Emulator, Integer>(Emulator.class, Integer.class) {
                @Override
                public Integer convert(Emulator fromObject) {
                    if (fromObject == null) {
                        return Integer.valueOf(-1);
                    }
                    return Integer.valueOf(viewModel.getEmulators().indexOf(fromObject));
                }
            });
            final var indexToEmulator = new UpdateValueStrategy<Integer, Emulator>();
            indexToEmulator.setConverter(new Converter<Integer, Emulator>(Integer.class, Emulator.class) {
                @Override
                public Emulator convert(Integer fromObject) {
                    final var idx = ((Integer) fromObject).intValue();
                    if (idx >= 0 && idx < viewModel.getEmulators().size()) {
                        return viewModel.getEmulators().get(idx);
                    }
                    return null;
                }
            });
            dbc.bindValue(emulatorSelection, emulatorIndexObservable, emulatorToIndex, indexToEmulator);

            emulatorIndexObservable.addValueChangeListener(e -> {
                final var idx = viewModel.getSelectedEmulatorIndex();
                if (idx >= 0 && idx < viewModel.getEmulators().size()) {
                    emulatorVersionsViewer.setInput(viewModel.getEmulators().get(idx).getVersions());
                } else {
                    emulatorVersionsViewer.setInput(List.of());
                }
            });
        }

        // emulator versions
        {
            final var emulatorVersionSelection =
                            ViewerProperties.singleSelection(String.class).observe(emulatorVersionsViewer);
            final var emulatorVersionIndexObservable = BeanProperties
                            .value(VenvWizardViewModel.class, "selectedEmulatorVersionIndex", Integer.class)
                            .observe(viewModel);
            final var emulatorVersionToIndex = new UpdateValueStrategy<String, Integer>();
            emulatorVersionToIndex.setConverter(new Converter<String, Integer>(String.class, Integer.class) {
                @Override
                public Integer convert(String fromObject) {
                    final var idx = viewModel.getSelectedEmulatorIndex();
                    if (idx < 0 || idx >= viewModel.getEmulators().size()) {
                        return Integer.valueOf(-1);
                    }
                    final var vers = viewModel.getEmulators().get(idx).getVersions();
                    return Integer.valueOf(vers == null ? -1 : vers.indexOf(fromObject));
                }
            });
            final var indexToEmulatorVersion = new UpdateValueStrategy<Integer, String>();
            indexToEmulatorVersion.setConverter(new Converter<Integer, String>(Integer.class, String.class) {
                @Override
                public String convert(Integer fromObject) {
                    final var idx = viewModel.getSelectedEmulatorIndex();
                    if (idx < 0 || idx >= viewModel.getEmulators().size()) {
                        return null;
                    }
                    final var vers = viewModel.getEmulators().get(idx).getVersions();
                    final var verIdx = ((Integer) fromObject).intValue();
                    return vers != null && verIdx >= 0 && verIdx < vers.size() ? vers.get(verIdx) : null;
                }
            });
            dbc.bindValue(emulatorVersionSelection, emulatorVersionIndexObservable, emulatorVersionToIndex,
                            indexToEmulatorVersion);
        }

        // sysroot
        {
            final var sysrootSelection = new SelectObservableValue<VenvWizardViewModel.SysrootOption>();
            sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.DEFAULT_SYSROOT,
                            WidgetProperties.buttonSelection().observe(sysrootDefaultRadio));
            sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.NONE_SYSROOT,
                            WidgetProperties.buttonSelection().observe(sysrootNoneRadio));
            sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.FOREIGN_TOOLCHAIN,
                            WidgetProperties.buttonSelection().observe(sysrootForeignRadio));

            dbc.bindValue(sysrootSelection, BeanProperties
                            .value(VenvWizardViewModel.class, "sysrootOption", VenvWizardViewModel.SysrootOption.class)
                            .observe(viewModel));
        }

        final var completeObservable =
                        BeanProperties.value(VenvWizardViewModel.class, "configurationPageComplete", Boolean.class)
                                        .observe(viewModel);
        completeObservable.addValueChangeListener(e -> updatePageComplete());
    }

    private void updatePageComplete() {
        setPageComplete(viewModel.isConfigurationPageComplete());
        if (getWizard() != null && getWizard().getContainer() != null) {
            getWizard().getContainer().updateButtons();
        }
    }
}
