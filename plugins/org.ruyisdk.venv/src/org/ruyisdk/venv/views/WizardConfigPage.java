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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.ruyisdk.ruyi.services.PackageIndexUpdater;
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
    private Button sysrootCopyFromDirectoryRadio;
    private Button sysrootSymlinkFromDirectoryRadio;
    private Button sysrootProjectFromRootfsRadio;
    private Link sysrootPackageLink;
    private Text sysrootDirectoryText;
    private Button sysrootDirectoryBrowseButton;

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
        {
            final var gridLayout = new GridLayout(2, false);
            gridLayout.makeColumnsEqualWidth = true;
            gridLayout.horizontalSpacing = 16;
            container.setLayout(gridLayout);
        }

        // Refresh button row
        {
            final var refreshComposite = new Composite(container, SWT.NONE);
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                refreshComposite.setLayoutData(gridData);
            }
            {
                final var gridLayout = new GridLayout(2, false);
                gridLayout.marginWidth = 0;
                refreshComposite.setLayout(gridLayout);
            }

            final var hintLabel = new Label(refreshComposite, SWT.NONE);
            hintLabel.setText(
                    "If lists are empty or contain outdated information, update the package index first.");
            hintLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            final var refreshButton = new Button(refreshComposite, SWT.PUSH);
            refreshButton.setText("Update Package Index");
            refreshButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performUpdateAndRefresh();
                }
            });
        }

        final var leftColumn = new Composite(container, SWT.NONE);
        leftColumn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        {
            final var gridLayout = new GridLayout(1, false);
            gridLayout.marginWidth = 0;
            leftColumn.setLayout(gridLayout);
        }

        final var rightColumn = new Composite(container, SWT.NONE);
        rightColumn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        {
            final var gridLayout = new GridLayout(1, false);
            gridLayout.marginWidth = 0;
            rightColumn.setLayout(gridLayout);
        }

        profileComposite = new Composite(leftColumn, SWT.NONE);
        profileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        {
            final var gridLayout = new GridLayout(1, false);
            gridLayout.marginWidth = 0;
            profileComposite.setLayout(gridLayout);
        }

        emulatorHeader = new Composite(leftColumn, SWT.NONE);
        emulatorHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        {
            final var gridLayout = new GridLayout(2, false);
            gridLayout.marginWidth = 0;
            emulatorHeader.setLayout(gridLayout);
        }

        emulatorComposite = new Composite(leftColumn, SWT.NONE);
        emulatorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        {
            final var gridLayout = new GridLayout(2, false);
            gridLayout.marginWidth = 0;
            emulatorComposite.setLayout(gridLayout);
        }

        toolchainComposite = new Composite(rightColumn, SWT.NONE);
        toolchainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        {
            final var gridLayout = new GridLayout(2, false);
            gridLayout.marginWidth = 0;
            toolchainComposite.setLayout(gridLayout);
        }

        sysrootGroup = new Group(rightColumn, SWT.NONE);
        sysrootGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sysrootGroup.setText("Sysroot Provisioning Options");
        {
            // columns: radio buttons, a package info link.
            final var gridLayout = new GridLayout(2, false);
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
                final var gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
                gridData.heightHint = 150;
                profileTable.setLayoutData(gridData);
            }
            profileTable.setHeaderVisible(true);
            profileTable.setLinesVisible(true);

            final var nameColumn = new TableViewerColumn(profileTableViewer, SWT.LEFT);
            {
                nameColumn.getColumn().setText("Name");
                nameColumn.getColumn().setWidth(200);
                nameColumn.setLabelProvider(new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        return ((Profile) element).getName();
                    }
                });
            }

            final var quirksColumn = new TableViewerColumn(profileTableViewer, SWT.LEFT);
            {
                quirksColumn.getColumn().setText("Needed Quirks");
                quirksColumn.getColumn().setWidth(400);
                quirksColumn.setLabelProvider(new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        final var q = ((Profile) element).getQuirks();
                        return q == null || q.isEmpty() ? "" : String.join(", ", q);
                    }
                });
            }

            profileTableViewer.setContentProvider(ArrayContentProvider.getInstance());

            final var profileComparator = new ViewerComparator() {
                private TableColumn sortColumn = nameColumn.getColumn();
                private int sortDirection = SWT.UP;

                public void setColumn(TableColumn column) {
                    if (column == sortColumn) {
                        sortDirection = (sortDirection == SWT.UP) ? SWT.DOWN : SWT.UP;
                    } else {
                        sortColumn = column;
                        sortDirection = SWT.UP;
                    }
                }

                public int getSortDirection() {
                    return sortDirection;
                }

                @Override
                public int compare(Viewer viewer, Object e1, Object e2) {
                    final var p1 = (Profile) e1;
                    final var p2 = (Profile) e2;
                    int result;
                    if (sortColumn == quirksColumn.getColumn()) {
                        final var q1 =
                                p1.getQuirks() == null ? "" : String.join(", ", p1.getQuirks());
                        final var q2 =
                                p2.getQuirks() == null ? "" : String.join(", ", p2.getQuirks());
                        result = q1.compareTo(q2);
                    } else {
                        final var n1 = p1.getName() == null ? "" : p1.getName();
                        final var n2 = p2.getName() == null ? "" : p2.getName();
                        result = n1.compareTo(n2);
                    }
                    return sortDirection == SWT.DOWN ? -result : result;
                }
            };
            profileTableViewer.setComparator(profileComparator);

            final var sortListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final var clickedColumn = (TableColumn) e.widget;
                    profileComparator.setColumn(clickedColumn);
                    profileTable.setSortColumn(clickedColumn);
                    profileTable.setSortDirection(profileComparator.getSortDirection());
                    profileTableViewer.refresh();
                }
            };
            nameColumn.getColumn().addSelectionListener(sortListener);
            quirksColumn.getColumn().addSelectionListener(sortListener);

            profileTable.setSortColumn(nameColumn.getColumn());
            profileTable.setSortDirection(SWT.UP);

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

            toolchainNamesViewer =
                    new TableViewer(toolchainComposite, SWT.BORDER | SWT.FULL_SELECTION);
            final var tcNames = toolchainNamesViewer.getTable();
            {
                final var gridData = new GridData(GridData.FILL_BOTH);
                gridData.heightHint = 120;
                gridData.widthHint = 300;
                tcNames.setLayoutData(gridData);
            }
            tcNames.setHeaderVisible(false);
            tcNames.setLinesVisible(true);

            toolchainVersionsViewer =
                    new TableViewer(toolchainComposite, SWT.BORDER | SWT.FULL_SELECTION);
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

            emulatorNamesViewer =
                    new TableViewer(emulatorComposite, SWT.BORDER | SWT.FULL_SELECTION);
            emulatorNames = emulatorNamesViewer.getTable();
            emulatorNames.setHeaderVisible(false);
            emulatorNames.setLinesVisible(true);
            final var enData = new GridData(GridData.FILL_BOTH);
            enData.heightHint = 80;
            enData.widthHint = 300;
            emulatorNames.setLayoutData(enData);

            emulatorVersionsViewer =
                    new TableViewer(emulatorComposite, SWT.BORDER | SWT.FULL_SELECTION);
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
            sysrootDefaultRadio.setText("Include sysroot from selected toolchain");
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                sysrootDefaultRadio.setLayoutData(gridData);
            }

            sysrootNoneRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootNoneRadio.setText("Do not include sysroot");
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                sysrootNoneRadio.setLayoutData(gridData);
            }

            sysrootForeignRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootForeignRadio.setText("Copy sysroot from specified package:");
            sysrootForeignRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

            sysrootPackageLink = new Link(sysrootGroup, SWT.NONE);
            sysrootPackageLink.setText("<a>(select package)</a>");
            sysrootPackageLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
            sysrootPackageLink.setEnabled(false);
            sysrootPackageLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openSysrootPackageDialog();
                }
            });

            sysrootCopyFromDirectoryRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootCopyFromDirectoryRadio.setText("Copy sysroot from directory");
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                sysrootCopyFromDirectoryRadio.setLayoutData(gridData);
            }

            sysrootSymlinkFromDirectoryRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootSymlinkFromDirectoryRadio.setText("Symlink sysroot from directory");
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                sysrootSymlinkFromDirectoryRadio.setLayoutData(gridData);
            }

            sysrootProjectFromRootfsRadio = new Button(sysrootGroup, SWT.RADIO);
            sysrootProjectFromRootfsRadio.setText("Project sysroot from rootfs directory");
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                sysrootProjectFromRootfsRadio.setLayoutData(gridData);
            }

            final var sysrootPathComposite = new Composite(sysrootGroup, SWT.NONE);
            {
                final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
                gridData.horizontalSpan = 2;
                sysrootPathComposite.setLayoutData(gridData);
            }
            {
                final var gridLayout = new GridLayout(2, false);
                gridLayout.marginWidth = 0;
                sysrootPathComposite.setLayout(gridLayout);
            }

            sysrootDirectoryText = new Text(sysrootPathComposite, SWT.BORDER);
            sysrootDirectoryText.setMessage("Select source directory path");
            sysrootDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            sysrootDirectoryText.setEnabled(false);

            sysrootDirectoryBrowseButton = new Button(sysrootPathComposite, SWT.PUSH);
            sysrootDirectoryBrowseButton.setText("Browse...");
            sysrootDirectoryBrowseButton.setEnabled(false);
            sysrootDirectoryBrowseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    openSysrootDirectoryDialog();
                }
            });
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

        final var profileIndexObservable = BeanProperties
                .value(VenvWizardViewModel.class, "selectedProfileIndex", Integer.class)
                .observe(viewModel);

        // profiles
        {
            final var profileSelection =
                    ViewerProperties.singleSelection(Profile.class).observe(profileTableViewer);
            final var profileToIndex = new UpdateValueStrategy<Profile, Integer>();
            profileToIndex
                    .setConverter(new Converter<Profile, Integer>(Profile.class, Integer.class) {
                        @Override
                        public Integer convert(Profile fromObject) {
                            if (fromObject == null) {
                                return Integer.valueOf(-1);
                            }
                            return Integer.valueOf(viewModel.getProfiles().indexOf(fromObject));
                        }
                    });
            final var indexToProfile = new UpdateValueStrategy<Integer, Profile>();
            indexToProfile
                    .setConverter(new Converter<Integer, Profile>(Integer.class, Profile.class) {
                        @Override
                        public Profile convert(Integer fromObject) {
                            final var idx = ((Integer) fromObject).intValue();
                            return idx >= 0 && idx < viewModel.getProfiles().size()
                                    ? viewModel.getProfiles().get(idx)
                                    : null;
                        }
                    });
            dbc.bindValue(profileSelection, profileIndexObservable, profileToIndex, indexToProfile);
        }

        // toolchain names
        {
            final var toolchainSelection =
                    ViewerProperties.singleSelection(Toolchain.class).observe(toolchainNamesViewer);
            final var toolchainIndexObservable = BeanProperties
                    .value(VenvWizardViewModel.class, "selectedToolchainIndex", Integer.class)
                    .observe(viewModel);
            final var toolchainToIndex = new UpdateValueStrategy<Toolchain, Integer>();
            toolchainToIndex.setConverter(
                    new Converter<Toolchain, Integer>(Toolchain.class, Integer.class) {
                        @Override
                        public Integer convert(Toolchain fromObject) {
                            if (fromObject == null) {
                                return Integer.valueOf(-1);
                            }
                            return Integer.valueOf(viewModel.getToolchains().indexOf(fromObject));
                        }
                    });
            final var indexToToolchain = new UpdateValueStrategy<Integer, Toolchain>();
            indexToToolchain.setConverter(
                    new Converter<Integer, Toolchain>(Integer.class, Toolchain.class) {
                        @Override
                        public Toolchain convert(Integer fromObject) {
                            final var idx = ((Integer) fromObject).intValue();
                            if (idx >= 0 && idx < viewModel.getToolchains().size()) {
                                return viewModel.getToolchains().get(idx);
                            }
                            return null;
                        }
                    });
            dbc.bindValue(toolchainSelection, toolchainIndexObservable, toolchainToIndex,
                    indexToToolchain);

            toolchainIndexObservable.addValueChangeListener(e -> {
                final var idx = viewModel.getSelectedToolchainIndex();
                if (idx >= 0 && idx < viewModel.getToolchains().size()) {
                    toolchainVersionsViewer
                            .setInput(viewModel.getToolchains().get(idx).getVersions());
                } else {
                    toolchainVersionsViewer.setInput(List.of());
                }
            });
        }

        // toolchain versions
        {
            final var toolchainVersionSelection =
                    ViewerProperties.singleSelection(String.class).observe(toolchainVersionsViewer);
            final var toolchainVersionIndexObservable =
                    BeanProperties.value(VenvWizardViewModel.class, "selectedToolchainVersionIndex",
                            Integer.class).observe(viewModel);
            final var toolchainVersionToIndex = new UpdateValueStrategy<String, Integer>();
            toolchainVersionToIndex
                    .setConverter(new Converter<String, Integer>(String.class, Integer.class) {
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
            indexToToolchainVersion
                    .setConverter(new Converter<Integer, String>(Integer.class, String.class) {
                        @Override
                        public String convert(Integer fromObject) {
                            final var idx = viewModel.getSelectedToolchainIndex();
                            if (idx < 0 || idx >= viewModel.getToolchains().size()) {
                                return null;
                            }
                            final var vers = viewModel.getToolchains().get(idx).getVersions();
                            final var verIdx = ((Integer) fromObject).intValue();
                            return vers != null && verIdx >= 0 && verIdx < vers.size()
                                    ? vers.get(verIdx)
                                    : null;
                        }
                    });
            dbc.bindValue(toolchainVersionSelection, toolchainVersionIndexObservable,
                    toolchainVersionToIndex, indexToToolchainVersion);
        }

        // emulator enablement
        {
            final var emulatorEnabledObservable = BeanProperties
                    .value(VenvWizardViewModel.class, "emulatorEnabled", Boolean.class)
                    .observe(viewModel);
            dbc.bindValue(WidgetProperties.buttonSelection().observe(emulatorCheckBox),
                    emulatorEnabledObservable);
            dbc.bindValue(WidgetProperties.enabled().observe(emulatorNames),
                    emulatorEnabledObservable);
            dbc.bindValue(WidgetProperties.enabled().observe(emulatorVersions),
                    emulatorEnabledObservable);
        }

        // emulator names
        {
            final var emulatorSelection =
                    ViewerProperties.singleSelection(Emulator.class).observe(emulatorNamesViewer);
            final var emulatorIndexObservable = BeanProperties
                    .value(VenvWizardViewModel.class, "selectedEmulatorIndex", Integer.class)
                    .observe(viewModel);
            final var emulatorToIndex = new UpdateValueStrategy<Emulator, Integer>();
            emulatorToIndex
                    .setConverter(new Converter<Emulator, Integer>(Emulator.class, Integer.class) {
                        @Override
                        public Integer convert(Emulator fromObject) {
                            if (fromObject == null) {
                                return Integer.valueOf(-1);
                            }
                            return Integer.valueOf(viewModel.getEmulators().indexOf(fromObject));
                        }
                    });
            final var indexToEmulator = new UpdateValueStrategy<Integer, Emulator>();
            indexToEmulator
                    .setConverter(new Converter<Integer, Emulator>(Integer.class, Emulator.class) {
                        @Override
                        public Emulator convert(Integer fromObject) {
                            final var idx = ((Integer) fromObject).intValue();
                            if (idx >= 0 && idx < viewModel.getEmulators().size()) {
                                return viewModel.getEmulators().get(idx);
                            }
                            return null;
                        }
                    });
            dbc.bindValue(emulatorSelection, emulatorIndexObservable, emulatorToIndex,
                    indexToEmulator);

            emulatorIndexObservable.addValueChangeListener(e -> {
                final var idx = viewModel.getSelectedEmulatorIndex();
                if (idx >= 0 && idx < viewModel.getEmulators().size()) {
                    emulatorVersionsViewer
                            .setInput(viewModel.getEmulators().get(idx).getVersions());
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
            emulatorVersionToIndex
                    .setConverter(new Converter<String, Integer>(String.class, Integer.class) {
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
            indexToEmulatorVersion
                    .setConverter(new Converter<Integer, String>(Integer.class, String.class) {
                        @Override
                        public String convert(Integer fromObject) {
                            final var idx = viewModel.getSelectedEmulatorIndex();
                            if (idx < 0 || idx >= viewModel.getEmulators().size()) {
                                return null;
                            }
                            final var vers = viewModel.getEmulators().get(idx).getVersions();
                            final var verIdx = ((Integer) fromObject).intValue();
                            return vers != null && verIdx >= 0 && verIdx < vers.size()
                                    ? vers.get(verIdx)
                                    : null;
                        }
                    });
            dbc.bindValue(emulatorVersionSelection, emulatorVersionIndexObservable,
                    emulatorVersionToIndex, indexToEmulatorVersion);
        }

        // filter toolchains and emulators by quirks when profile changes
        profileIndexObservable.addValueChangeListener(e -> {
            toolchainNamesViewer.setInput(viewModel.getToolchains());
            emulatorNamesViewer.setInput(viewModel.getEmulators());
        });

        // sysroot
        {
            final var sysrootSelection =
                    new SelectObservableValue<VenvWizardViewModel.SysrootOption>();
            {
                sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.DEFAULT_SYSROOT,
                        WidgetProperties.buttonSelection().observe(sysrootDefaultRadio));
                sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.NONE_SYSROOT,
                        WidgetProperties.buttonSelection().observe(sysrootNoneRadio));
                sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.FOREIGN_TOOLCHAIN,
                        WidgetProperties.buttonSelection().observe(sysrootForeignRadio));
                sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.COPY_FROM_DIRECTORY,
                        WidgetProperties.buttonSelection().observe(sysrootCopyFromDirectoryRadio));
                sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.SYMLINK_FROM_DIRECTORY,
                        WidgetProperties.buttonSelection()
                                .observe(sysrootSymlinkFromDirectoryRadio));
                sysrootSelection.addOption(VenvWizardViewModel.SysrootOption.PROJECT_FROM_ROOTFS,
                        WidgetProperties.buttonSelection().observe(sysrootProjectFromRootfsRadio));
            }
            final var sysrootOptionObservable = BeanProperties.value(VenvWizardViewModel.class,
                    "sysrootOption", VenvWizardViewModel.SysrootOption.class).observe(viewModel);
            final var defaultSysrootOptionAvailableObservable =
                    BeanProperties.value(VenvWizardViewModel.class, "defaultSysrootOptionAvailable",
                            Boolean.class).observe(viewModel);
            final var displayTextObservable = BeanProperties
                    .value(VenvWizardViewModel.class, "sysrootPackageDisplayText", String.class)
                    .observe(viewModel);
            final var sysrootDirectoryObservable = BeanProperties
                    .value(VenvWizardViewModel.class, "sysrootDirectoryPath", String.class)
                    .observe(viewModel);
            final var sysrootDefaultEnabled =
                    WidgetProperties.enabled().observe(sysrootDefaultRadio);

            dbc.bindValue(sysrootSelection, sysrootOptionObservable);
            dbc.bindValue(sysrootDefaultEnabled, defaultSysrootOptionAvailableObservable);
            dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(sysrootDirectoryText),
                    sysrootDirectoryObservable);

            sysrootOptionObservable.addValueChangeListener(e -> {
                final var fromPkg = viewModel
                        .getSysrootOption() == VenvWizardViewModel.SysrootOption.FOREIGN_TOOLCHAIN;
                final var fromDirectory = switch (viewModel.getSysrootOption()) {
                    case VenvWizardViewModel.SysrootOption.COPY_FROM_DIRECTORY -> true;
                    case VenvWizardViewModel.SysrootOption.SYMLINK_FROM_DIRECTORY -> true;
                    case VenvWizardViewModel.SysrootOption.PROJECT_FROM_ROOTFS -> true;
                    default -> false;
                };
                sysrootPackageLink.setEnabled(fromPkg);
                sysrootDirectoryText.setEnabled(fromDirectory);
                sysrootDirectoryBrowseButton.setEnabled(fromDirectory);
            });

            displayTextObservable.addValueChangeListener(e -> {
                final var text = viewModel.getSysrootPackageDisplayText();
                if (text == null || text.isEmpty()) {
                    sysrootPackageLink.setText("<a>(select package)</a>");
                } else {
                    sysrootPackageLink.setText("<a>" + text + "</a>");
                }
                sysrootGroup.requestLayout();
            });
        }

        final var completeObservable = BeanProperties
                .value(VenvWizardViewModel.class, "configurationPageComplete", Boolean.class)
                .observe(viewModel);
        completeObservable.addValueChangeListener(e -> updatePageComplete());
    }

    private void updatePageComplete() {
        setPageComplete(viewModel.isConfigurationPageComplete());
        if (getWizard() != null && getWizard().getContainer() != null) {
            getWizard().getContainer().updateButtons();
        }
    }

    private void openSysrootPackageDialog() {
        final var dialog = new SysrootPackageSelectionDialog(getShell(), viewModel);
        dialog.open();
    }

    private void openSysrootDirectoryDialog() {
        final var dialog = new DirectoryDialog(getShell());
        dialog.setText("Select sysroot source directory");
        dialog.setMessage("Choose a source directory for sysroot provisioning.");
        final var currentPath = viewModel.getSysrootDirectoryPath();
        if (currentPath != null && !currentPath.isBlank()) {
            dialog.setFilterPath(currentPath);
        }
        final var selectedPath = dialog.open();
        if (selectedPath != null) {
            viewModel.setSysrootDirectoryPath(selectedPath);
        }
    }

    private void performUpdateAndRefresh() {
        if (!PackageIndexUpdater.updateWithProgress(getShell())) {
            return;
        }

        viewModel.loadAll();
        profileTableViewer.setInput(viewModel.getProfiles());
        toolchainNamesViewer.setInput(viewModel.getToolchains());
        toolchainVersionsViewer.setInput(List.of());
        emulatorNamesViewer.setInput(viewModel.getEmulators());
        emulatorVersionsViewer.setInput(List.of());
    }
}
