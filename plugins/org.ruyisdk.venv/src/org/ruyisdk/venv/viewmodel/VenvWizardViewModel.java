package org.ruyisdk.venv.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.ruyisdk.ruyi.services.RuyiCliException;
import org.ruyisdk.venv.model.Emulator;
import org.ruyisdk.venv.model.Profile;
import org.ruyisdk.venv.model.Toolchain;
import org.ruyisdk.venv.model.VenvDetectionService;

/** View model backing the venv creation wizard UI. */
public class VenvWizardViewModel {

    private static final DateTimeFormatter VENV_NAME_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final VenvDetectionService service;

    private boolean configurationPageComplete;
    private boolean defaultSysrootOptionAvailable;
    private String summaryText = "";

    private boolean dataLoading;
    private String loadingMessage = "";
    private String loadingErrorMessage = "";
    private boolean dataLoadStarted;

    private final IObservableList<Profile> profiles =
            new WritableList<>(new ArrayList<>(), Profile.class);
    private int selectedProfileIndex = -1;

    private final IObservableList<Toolchain> toolchains =
            new WritableList<>(new ArrayList<>(), Toolchain.class);
    private final List<Toolchain> allToolchains = new ArrayList<>();
    private int selectedToolchainIndex = -1;
    private int selectedToolchainVersionIndex = -1;
    private final IObservableList<String> toolchainVersions =
            new WritableList<>(new ArrayList<>(), String.class);

    private final IObservableList<Emulator> emulators =
            new WritableList<>(new ArrayList<>(), Emulator.class);
    private final List<Emulator> allEmulators = new ArrayList<>();
    private int selectedEmulatorIndex = -1;
    private int selectedEmulatorVersionIndex = -1;
    private boolean emulatorEnabled = false;
    private final IObservableList<String> emulatorVersions =
            new WritableList<>(new ArrayList<>(), String.class);

    private SysrootOption sysrootOption = SysrootOption.DEFAULT_SYSROOT;
    private int selectedSysrootPackageIndex = -1;
    private int selectedSysrootPackageVersionIndex = -1;
    private String sysrootPackageDisplayText = "";
    private String sysrootDirectoryPath = "";

    private String venvLocation = "";
    private boolean venvLocationReadOnly = false;
    private String venvName = "";
    private boolean venvNameManuallyOverridden = false;

    private final IObservableList<String> projectRootPaths =
            new WritableList<>(new ArrayList<>(), String.class);

    /** Available sysroot selection strategies. */
    public enum SysrootOption {
        /**
         * Do not include a sysroot. Don't use this as fallback, otherwise new users may be
         * confused.
         */
        NONE_SYSROOT,
        /** Use the sysroot included with the selected toolchain. */
        DEFAULT_SYSROOT,
        /** Use the sysroot from another installed package. */
        FOREIGN_TOOLCHAIN,
        /** Copy sysroot from an existing directory. */
        COPY_FROM_DIRECTORY,
        /** Symlink sysroot to an existing directory. */
        SYMLINK_FROM_DIRECTORY,
        /** Project sysroot from a distro rootfs directory. */
        PROJECT_FROM_ROOTFS
    }

    private static boolean usesSysrootDirectory(SysrootOption option) {
        return switch (option) {
            case SysrootOption.COPY_FROM_DIRECTORY -> true;
            case SysrootOption.SYMLINK_FROM_DIRECTORY -> true;
            case SysrootOption.PROJECT_FROM_ROOTFS -> true;
            default -> false;
        };
    }

    /** Creates a new view model instance. */
    public VenvWizardViewModel(VenvDetectionService service) {
        this.service = service;
    }

    private void recomputeDerivedState() {
        recomputeDefaultSysrootOptionAvailable();
        enforceDefaultSysrootOptionAvailable();
        updateSummaryText();
        recomputeConfigurationPageComplete();
    }

    private void recomputeDefaultSysrootOptionAvailable() {
        final var old = this.defaultSysrootOptionAvailable;
        this.defaultSysrootOptionAvailable =
                selectedToolchainIndex >= 0 && selectedToolchainIndex < toolchains.size()
                        && toolchains.get(selectedToolchainIndex).hasIncludedSysroot();
        pcs.firePropertyChange("defaultSysrootOptionAvailable", old,
                this.defaultSysrootOptionAvailable);
    }

    private void enforceDefaultSysrootOptionAvailable() {
        if (sysrootOption == SysrootOption.DEFAULT_SYSROOT && !defaultSysrootOptionAvailable) {
            setSysrootOption(SysrootOption.FOREIGN_TOOLCHAIN);
        }
    }

    private void updateSummaryText() {
        final var old = this.summaryText;
        this.summaryText = buildSummaryText();
        pcs.firePropertyChange("summaryText", old, this.summaryText);
    }

    private void recomputeConfigurationPageComplete() {
        final var old = this.configurationPageComplete;
        this.configurationPageComplete = computeConfigurationPageComplete();
        pcs.firePropertyChange("configurationPageComplete", old, this.configurationPageComplete);
    }

    private boolean computeConfigurationPageComplete() {
        if (!(selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size())) {
            return false;
        }

        if (!(selectedToolchainIndex >= 0 && selectedToolchainIndex < toolchains.size())) {
            return false;
        }

        final var toolchainVersions = toolchains.get(selectedToolchainIndex).getVersions();

        if (!(toolchainVersions != null && selectedToolchainVersionIndex >= 0
                && selectedToolchainVersionIndex < toolchainVersions.size())) {
            return false;
        }

        if (sysrootOption == SysrootOption.FOREIGN_TOOLCHAIN) {
            if (!isSysrootPackageSelected()) {
                return false;
            }
        } else if (usesSysrootDirectory(sysrootOption)) {
            if (sysrootDirectoryPath == null || sysrootDirectoryPath.isBlank()) {
                return false;
            }
        }

        if (!emulatorEnabled) {
            return true;
        }

        // emulator enabled

        if (!(selectedEmulatorIndex >= 0 && selectedEmulatorIndex < emulators.size())) {
            return false;
        }

        final var emulatorVersions = emulators.get(selectedEmulatorIndex).getVersions();

        if (!(emulatorVersions != null && selectedEmulatorVersionIndex >= 0
                && selectedEmulatorVersionIndex < emulatorVersions.size())) {
            return false;
        }

        return true;
    }

    private boolean isSysrootPackageSelected() {
        final var sysrootToolchains = getSysrootToolchains();
        if (!(selectedSysrootPackageIndex >= 0
                && selectedSysrootPackageIndex < sysrootToolchains.size())) {
            return false;
        }
        final var versions = sysrootToolchains.get(selectedSysrootPackageIndex).getVersions();
        return versions != null && selectedSysrootPackageVersionIndex >= 0
                && selectedSysrootPackageVersionIndex < versions.size();
    }

    private String buildSummaryText() {
        final var sb = new StringBuilder();
        sb.append("Profile: ");
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            final var profile = profiles.get(selectedProfileIndex);
            sb.append(profile.getName());
            final var quirks = profile.getQuirks();
            if (quirks != null && !quirks.isEmpty()) {
                sb.append(" (quirks: ").append(String.join(", ", quirks)).append(")");
            }
        }
        sb.append('\n');

        sb.append("Toolchain: ");
        if (selectedToolchainIndex >= 0 && selectedToolchainIndex < toolchains.size()) {
            final var toolchain = toolchains.get(selectedToolchainIndex);
            sb.append(toolchain.getName());
            final var versions = toolchain.getVersions();
            if (selectedToolchainVersionIndex >= 0 && versions != null
                    && selectedToolchainVersionIndex < versions.size()) {
                sb.append(" (").append(versions.get(selectedToolchainVersionIndex)).append(")");
            }
        }
        sb.append('\n');

        sb.append("Emulator: ");
        if (!emulatorEnabled) {
            sb.append("disabled");
        } else if (selectedEmulatorIndex >= 0 && selectedEmulatorIndex < emulators.size()) {
            final var emulator = emulators.get(selectedEmulatorIndex);
            sb.append(emulator.getName());
            final var versions = emulator.getVersions();
            if (selectedEmulatorVersionIndex >= 0 && versions != null
                    && selectedEmulatorVersionIndex < versions.size()) {
                sb.append(" (").append(versions.get(selectedEmulatorVersionIndex)).append(")");
            }
        }
        sb.append('\n');

        sb.append("Sysroot: ");
        if (sysrootOption == SysrootOption.DEFAULT_SYSROOT) {
            sb.append("using included sysroot");
        } else if (sysrootOption == SysrootOption.NONE_SYSROOT) {
            sb.append("none");
        } else if (sysrootOption == SysrootOption.FOREIGN_TOOLCHAIN) {
            if (isSysrootPackageSelected()) {
                final var pkg = getSysrootToolchains().get(selectedSysrootPackageIndex);
                final var ver = pkg.getVersions().get(selectedSysrootPackageVersionIndex);
                sb.append(String.format("copy from %s(%s)", pkg.getName(), ver));
            }
        } else if (sysrootOption == SysrootOption.COPY_FROM_DIRECTORY) {
            sb.append("copy from directory: ").append(sysrootDirectoryPath);
        } else if (sysrootOption == SysrootOption.SYMLINK_FROM_DIRECTORY) {
            sb.append("symlink from directory: ").append(sysrootDirectoryPath);
        } else if (sysrootOption == SysrootOption.PROJECT_FROM_ROOTFS) {
            sb.append("project from rootfs: ").append(sysrootDirectoryPath);
        } else {
            sb.append(sysrootOption.toString());
        }
        return sb.toString();
    }

    /** Fetched package data, transferred from the background job to the UI thread. */
    private record FetchedData(List<Profile> profiles, List<Toolchain> toolchains,
            List<Emulator> emulators) {
    }

    private FetchedData fetchData() {
        final var fetchedProfiles = new ArrayList<Profile>();
        final var profileInfos = service.listProfiles();
        if (profileInfos != null) {
            for (final var profileInfo : profileInfos) {
                fetchedProfiles.add(new Profile(profileInfo.getName(), profileInfo.getQuirks()));
            }
        }

        final var fetchedToolchains = new ArrayList<Toolchain>();
        final var toolchainInfos = service.listToolchains();
        if (toolchainInfos != null) {
            for (final var toolchainInfo : toolchainInfos) {
                fetchedToolchains
                        .add(new Toolchain(toolchainInfo.getName(), toolchainInfo.getVersions(),
                                toolchainInfo.getQuirks(), toolchainInfo.hasIncludedSysroot()));
            }
        }

        final var fetchedEmulators = new ArrayList<Emulator>();
        final var emulatorInfos = service.listEmulators();
        if (emulatorInfos != null) {
            for (final var emulatorInfo : emulatorInfos) {
                fetchedEmulators.add(new Emulator(emulatorInfo.getName(),
                        emulatorInfo.getVersions(), emulatorInfo.getQuirks()));
            }
        }

        return new FetchedData(fetchedProfiles, fetchedToolchains, fetchedEmulators);
    }

    private void applyFetchedData(FetchedData data) {
        final Runnable update = () -> {
            profiles.clear();
            profiles.addAll(data.profiles());
            allToolchains.clear();
            allToolchains.addAll(data.toolchains());
            allEmulators.clear();
            allEmulators.addAll(data.emulators());
            repopulatePackagesByProfile();
            recomputeDerivedState();
        };
        // Observable lists may only be mutated on their own realm.
        if (profiles.getRealm().isCurrent()) {
            update.run();
        } else {
            profiles.getRealm().asyncExec(update);
        }
    }

    /** Loads package lists from the Ruyi CLI and refreshes all view model data. */
    public void loadAll() {
        applyFetchedData(fetchData());
    }

    /**
     * Loads package lists from the Ruyi CLI asynchronously, keeping the UI responsive. Progress and
     * failures are reported through the {@code dataLoading}, {@code loadingMessage} and
     * {@code loadingErrorMessage} properties. Does nothing if a load is already running.
     */
    public void loadAllAsync() {
        if (dataLoading) {
            return;
        }
        dataLoadStarted = true;

        runOnUiThread(() -> {
            setLoadingErrorMessage("");
            setDataLoading(true);
            setLoadingMessage("Loading package data...");
        });

        final var job = Job.create("Loading package data", monitor -> {
            if (monitor.isCanceled()) {
                runOnUiThread(() -> {
                    setDataLoading(false);
                    setLoadingMessage("Loading cancelled.");
                });
                return Status.CANCEL_STATUS;
            }
            try {
                final var fetched = fetchData();
                runOnUiThread(() -> {
                    applyFetchedData(fetched);
                    setDataLoading(false);
                    setLoadingMessage("");
                    setLoadingErrorMessage("");
                });
                return Status.OK_STATUS;
            } catch (Exception e) {
                final var message = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> {
                    setDataLoading(false);
                    setLoadingMessage("");
                    setLoadingErrorMessage(message);
                });
                return new Status(IStatus.ERROR, "org.ruyisdk.venv", "Failed to load package data",
                        e);
            }
        });
        job.schedule();
    }

    private static void runOnUiThread(Runnable runnable) {
        final var display = Display.getDefault();
        if (display == null || display.isDisposed()) {
            return;
        }
        if (display.getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            display.asyncExec(runnable);
        }
    }

    /**
     * Rebuilds the toolchain and emulator lists to match the selected profile's quirks; when no
     * profile is selected, all packages are shown. Then resets the toolchain, sysroot, and emulator
     * selections since the lists changed. The selection setters also empty the toolchain and
     * emulator version lists.
     */
    private void repopulatePackagesByProfile() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            toolchains.clear();
            toolchains.addAll(allToolchains);
            emulators.clear();
            emulators.addAll(allEmulators);
        } else {
            final var profileQuirks = profiles.get(selectedProfileIndex).getQuirks();

            toolchains.clear();
            for (final var tc : allToolchains) {
                if (quirksMatch(profileQuirks, tc.getQuirks())) {
                    toolchains.add(tc);
                }
            }

            emulators.clear();
            for (final var em : allEmulators) {
                if (quirksMatch(profileQuirks, em.getQuirks())) {
                    emulators.add(em);
                }
            }
        }

        // Reset any other selections since the data changed. Version lists will be emptied by the
        // selection setters.
        setSelectedToolchainIndex(-1);
        setSelectedSysrootPackageIndex(-1);
        setSelectedEmulatorIndex(-1);
    }

    private static boolean quirksMatch(List<String> profileQuirks, List<String> packageQuirks) {
        final var neededByProfile =
                profileQuirks == null ? Set.<String>of() : new HashSet<>(profileQuirks);
        final var providedByPackage =
                packageQuirks == null ? Set.<String>of() : new HashSet<>(packageQuirks);
        if (neededByProfile.isEmpty()) {
            return providedByPackage.isEmpty();
        }
        return providedByPackage.containsAll(neededByProfile);
    }

    private void installToolchain(String name, String version) {
        service.installPackage(name, version);
    }

    private void installEmulator(String name, String version) {
        service.installPackage(name, version);
    }

    private void installPackageForSysroot(String name, String version) {
        service.installPackage(name, version);
    }

    /**
     * Data required by the finalization operations, resolved on the realm thread in advance. A null
     * String component means the corresponding wizard option was not selected.
     */
    private static record FinalizationData(String toolchainName, String toolchainVersion,
            String profile, SysrootOption sysrootOption, String sysrootPackageName,
            String sysrootPackageVersion, String sysrootDirectory, boolean emulatorEnabled,
            String emulatorName, String emulatorVersion, String venvLocation, String venvName) {
    }

    private FinalizationData finalizationData;

    /**
     * Serialize everything needed by the finalization operations. Must be called on the realm of
     * the observable lists (i.e. the UI thread) before {@link #doFinalization(Consumer)} is invoked
     * from another thread.
     */
    public void buildFinalizationData() {
        if (venvLocation == null || venvLocation.isBlank()) {
            throw RuyiCliException.invalidArgument("Venv parent path is empty");
        }
        if (venvName == null || venvName.isBlank()) {
            throw RuyiCliException.invalidArgument("Venv name is empty");
        }

        if (selectedToolchainIndex < 0 || selectedToolchainIndex >= toolchains.size()
                || selectedToolchainVersionIndex < 0) {
            throw RuyiCliException.invalidArgument("No toolchain selected");
        }
        final var toolchain = toolchains.get(selectedToolchainIndex);
        final var toolchainName = toolchain.getName();
        final var toolchainVersion = toolchain.getVersions().get(selectedToolchainVersionIndex);

        String profile = null;
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            profile = profiles.get(selectedProfileIndex).getName();
        }

        String sysrootPackageName = null;
        String sysrootPackageVersion = null;
        String sysrootDirectory = null;
        if (sysrootOption == SysrootOption.FOREIGN_TOOLCHAIN) {
            if (!isSysrootPackageSelected()) {
                throw RuyiCliException
                        .invalidArgument("Sysroot from package selected but no package chosen");
            }
            final var pkg = getSysrootToolchains().get(selectedSysrootPackageIndex);
            sysrootPackageName = pkg.getName();
            sysrootPackageVersion = pkg.getVersions().get(selectedSysrootPackageVersionIndex);
        } else if (usesSysrootDirectory(sysrootOption)) {
            if (sysrootDirectoryPath == null || sysrootDirectoryPath.isBlank()) {
                throw RuyiCliException
                        .invalidArgument("Sysroot directory option selected but no path set");
            }
            sysrootDirectory = sysrootDirectoryPath;
        }

        String emulatorName = null;
        String emulatorVersion = null;
        if (emulatorEnabled) {
            if (selectedEmulatorIndex < 0 || selectedEmulatorIndex >= emulators.size()
                    || selectedEmulatorVersionIndex < 0) {
                throw RuyiCliException.invalidArgument("Emulator enabled but not selected");
            }
            final var emulator = emulators.get(selectedEmulatorIndex);
            emulatorName = emulator.getName();
            emulatorVersion = emulator.getVersions().get(selectedEmulatorVersionIndex);
        }

        finalizationData = new FinalizationData(toolchainName, toolchainVersion, profile,
                sysrootOption, sysrootPackageName, sysrootPackageVersion, sysrootDirectory,
                emulatorEnabled, emulatorName, emulatorVersion, venvLocation, venvName);
    }

    /**
     * Creates the virtual environment and installs its dependencies from the data recorded by
     * {@link #buildFinalizationData()}, reporting each step through {@code stepReporter}. May be
     * called from any thread since it only touches the snapshotted plain values. The
     * {@link #finalizationData} field is cleared before returning.
     *
     * @param stepReporter callback receiving a short description of each step, e.g. for
     *        {@code IProgressMonitor#subTask}
     */
    public void doFinalization(Consumer<String> stepReporter) {
        if (finalizationData == null) {
            throw RuyiCliException.invalidArgument("Finalization data not ready");
        }
        final var data = finalizationData;
        finalizationData = null;

        stepReporter.accept("install toolchain");
        installToolchain(data.toolchainName(), data.toolchainVersion());

        if (data.sysrootOption() == SysrootOption.FOREIGN_TOOLCHAIN) {
            stepReporter.accept("install package for sysroot");
            installPackageForSysroot(data.sysrootPackageName(), data.sysrootPackageVersion());
        }

        if (data.emulatorEnabled()) {
            stepReporter.accept("install emulator");
            installEmulator(data.emulatorName(), data.emulatorVersion());
        }

        stepReporter.accept("create venv");

        Boolean withSysroot = null;
        String sysrootFromPackage = null;
        String copySysrootFromDir = null;
        String symlinkSysrootFromDir = null;
        String projectSysrootFromRootfs = null;
        if (data.sysrootOption() == SysrootOption.DEFAULT_SYSROOT) {
            withSysroot = true;
        } else if (data.sysrootOption() == SysrootOption.NONE_SYSROOT) {
            withSysroot = false;
        } else if (data.sysrootOption() == SysrootOption.FOREIGN_TOOLCHAIN) {
            sysrootFromPackage = String.format("%s(%s)", data.sysrootPackageName(),
                    data.sysrootPackageVersion());
        } else if (data.sysrootOption() == SysrootOption.COPY_FROM_DIRECTORY) {
            copySysrootFromDir = data.sysrootDirectory();
        } else if (data.sysrootOption() == SysrootOption.SYMLINK_FROM_DIRECTORY) {
            symlinkSysrootFromDir = data.sysrootDirectory();
        } else if (data.sysrootOption() == SysrootOption.PROJECT_FROM_ROOTFS) {
            projectSysrootFromRootfs = data.sysrootDirectory();
        }

        final var path = new File(data.venvLocation(), data.venvName()).getPath();
        service.createVenv(path, data.toolchainName(), data.toolchainVersion(), data.profile(),
                withSysroot, sysrootFromPackage, copySysrootFromDir, symlinkSysrootFromDir,
                projectSysrootFromRootfs, data.emulatorName(), data.emulatorVersion());
    }

    /** Returns available profiles as an observable list. */
    public IObservableList<Profile> getProfiles() {
        return profiles;
    }

    /** Returns the selected profile index. */
    public int getSelectedProfileIndex() {
        return selectedProfileIndex;
    }

    /** Sets the selected profile index. */
    public void setSelectedProfileIndex(int index) {
        final var old = this.selectedProfileIndex;
        this.selectedProfileIndex = index;
        pcs.firePropertyChange("selectedProfileIndex", old, this.selectedProfileIndex);
        applyDefaultVenvNameForSelectedProfile();
        repopulatePackagesByProfile();
        recomputeDerivedState();
    }

    private String buildDefaultVenvNameForSelectedProfile() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            return "";
        }
        final var profile = profiles.get(selectedProfileIndex);
        if (profile == null || profile.getName() == null || profile.getName().isBlank()) {
            return "";
        }
        // https://github.com/ruyisdk/ruyisdk-vscode-extension/blob/0.1.4/src/venv/create.command.ts#L217-L229
        // b7b4ab08ea1907db517c27993e857c300fc4a983
        return "ruyi-venv-" + profile.getName().replaceAll("\\s+", "-") + "-"
                + ZonedDateTime.now().format(VENV_NAME_TIMESTAMP_FORMAT);
    }

    private void applyDefaultVenvNameForSelectedProfile() {
        if (venvNameManuallyOverridden) {
            return;
        }
        final var defaultName = buildDefaultVenvNameForSelectedProfile();
        if (defaultName.isBlank()) {
            return;
        }
        // Time ticks. It should not be considered as a manual override.
        setVenvNameInternal(defaultName, false);
    }

    /** Returns available toolchains as an observable list. */
    public IObservableList<Toolchain> getToolchains() {
        return toolchains;
    }

    /** Returns the selected toolchain index. */
    public int getSelectedToolchainIndex() {
        return selectedToolchainIndex;
    }

    /** Sets the selected toolchain index and updates the version list. */
    public void setSelectedToolchainIndex(int index) {
        final var old = this.selectedToolchainIndex;
        this.selectedToolchainIndex = index;
        pcs.firePropertyChange("selectedToolchainIndex", old, this.selectedToolchainIndex);

        // If both values are -1, we don't need to update the already empty version list.
        if (old != index) {
            updateToolchainVersions();
            setSelectedToolchainVersionIndex(-1);
        }

        recomputeDerivedState();
    }

    private void updateToolchainVersions() {
        toolchainVersions.clear();
        if (selectedToolchainIndex >= 0 && selectedToolchainIndex < toolchains.size()) {
            final var versions = toolchains.get(selectedToolchainIndex).getVersions();
            if (versions != null) {
                toolchainVersions.addAll(versions);
            }
        }
    }

    /** Returns the versions of the selected toolchain as an observable list. */
    public IObservableList<String> getToolchainVersions() {
        return toolchainVersions;
    }

    /** Returns the selected toolchain version index. */
    public int getSelectedToolchainVersionIndex() {
        return selectedToolchainVersionIndex;
    }

    /** Sets the selected toolchain version index. */
    public void setSelectedToolchainVersionIndex(int index) {
        final var old = this.selectedToolchainVersionIndex;
        this.selectedToolchainVersionIndex = index;
        pcs.firePropertyChange("selectedToolchainVersionIndex", old,
                this.selectedToolchainVersionIndex);
        recomputeDerivedState();
    }

    /** Returns available emulators as an observable list. */
    public IObservableList<Emulator> getEmulators() {
        return emulators;
    }

    /** Returns the selected emulator index. */
    public int getSelectedEmulatorIndex() {
        return selectedEmulatorIndex;
    }

    /** Sets the selected emulator index and updates the version list. */
    public void setSelectedEmulatorIndex(int index) {
        final var old = this.selectedEmulatorIndex;
        this.selectedEmulatorIndex = index;
        pcs.firePropertyChange("selectedEmulatorIndex", old, this.selectedEmulatorIndex);

        // If both values are -1, we don't need to update the already empty version list.
        if (old != index) {
            updateEmulatorVersions();
            setSelectedEmulatorVersionIndex(-1);
        }

        recomputeDerivedState();
    }

    private void updateEmulatorVersions() {
        emulatorVersions.clear();
        if (selectedEmulatorIndex >= 0 && selectedEmulatorIndex < emulators.size()) {
            final var versions = emulators.get(selectedEmulatorIndex).getVersions();
            if (versions != null) {
                emulatorVersions.addAll(versions);
            }
        }
    }

    /** Returns the versions of the selected emulator as an observable list. */
    public IObservableList<String> getEmulatorVersions() {
        return emulatorVersions;
    }

    /** Returns the selected emulator version index. */
    public int getSelectedEmulatorVersionIndex() {
        return selectedEmulatorVersionIndex;
    }

    /** Sets the selected emulator version index. */
    public void setSelectedEmulatorVersionIndex(int index) {
        final var old = this.selectedEmulatorVersionIndex;
        this.selectedEmulatorVersionIndex = index;
        pcs.firePropertyChange("selectedEmulatorVersionIndex", old,
                this.selectedEmulatorVersionIndex);
        recomputeDerivedState();
    }

    /** Returns whether emulator selection is enabled. */
    public boolean isEmulatorEnabled() {
        return emulatorEnabled;
    }

    /** Enables or disables emulator selection. */
    public void setEmulatorEnabled(boolean enabled) {
        final var old = this.emulatorEnabled;
        this.emulatorEnabled = enabled;
        pcs.firePropertyChange("emulatorEnabled", old, this.emulatorEnabled);
        if (!enabled) {
            setSelectedEmulatorIndex(-1);
            setSelectedEmulatorVersionIndex(-1);
        }
        recomputeDerivedState();
    }

    /** Returns the sysroot selection option. */
    public SysrootOption getSysrootOption() {
        return sysrootOption;
    }

    /** Returns whether the selected toolchain can provide the default sysroot. */
    public boolean isDefaultSysrootOptionAvailable() {
        return defaultSysrootOptionAvailable;
    }

    /** Sets the sysroot selection option. */
    public void setSysrootOption(SysrootOption option) {
        if (option == SysrootOption.DEFAULT_SYSROOT && !defaultSysrootOptionAvailable) {
            option = SysrootOption.FOREIGN_TOOLCHAIN;
        }
        final var old = this.sysrootOption;
        this.sysrootOption = option;
        pcs.firePropertyChange("sysrootOption", old, this.sysrootOption);
        if (option != SysrootOption.FOREIGN_TOOLCHAIN) {
            setSelectedSysrootPackageIndex(-1);
            setSelectedSysrootPackageVersionIndex(-1);
        }
        if (!usesSysrootDirectory(option)) {
            setSysrootDirectoryPath("");
        }
        recomputeDerivedState();
    }

    /** Returns the selected sysroot package index within the toolchains list. */
    public int getSelectedSysrootPackageIndex() {
        return selectedSysrootPackageIndex;
    }

    /** Sets the selected sysroot package index within the toolchains list. */
    public void setSelectedSysrootPackageIndex(int index) {
        final var sysrootToolchains = getSysrootToolchains();
        if (index < -1 || index >= sysrootToolchains.size()) {
            index = -1;
        }
        final var old = this.selectedSysrootPackageIndex;
        this.selectedSysrootPackageIndex = index;
        pcs.firePropertyChange("selectedSysrootPackageIndex", old,
                this.selectedSysrootPackageIndex);
        if (old != index) {
            setSelectedSysrootPackageVersionIndex(-1);
        }
        updateSysrootPackageDisplayText();
        recomputeDerivedState();
    }

    /** Returns the selected sysroot package version index. */
    public int getSelectedSysrootPackageVersionIndex() {
        return selectedSysrootPackageVersionIndex;
    }

    /** Sets the selected sysroot package version index. */
    public void setSelectedSysrootPackageVersionIndex(int index) {
        final var old = this.selectedSysrootPackageVersionIndex;
        this.selectedSysrootPackageVersionIndex = index;
        pcs.firePropertyChange("selectedSysrootPackageVersionIndex", old,
                this.selectedSysrootPackageVersionIndex);
        updateSysrootPackageDisplayText();
        recomputeDerivedState();
    }

    /** Returns the display text describing the selected sysroot package. */
    public String getSysrootPackageDisplayText() {
        return sysrootPackageDisplayText;
    }

    /** Returns the selected sysroot directory path for directory-based options. */
    public String getSysrootDirectoryPath() {
        return sysrootDirectoryPath;
    }

    /** Sets the selected sysroot directory path for directory-based options. */
    public void setSysrootDirectoryPath(String path) {
        final var normalized = path == null ? "" : path;
        final var old = this.sysrootDirectoryPath;
        this.sysrootDirectoryPath = normalized;
        pcs.firePropertyChange("sysrootDirectoryPath", old, this.sysrootDirectoryPath);
        recomputeDerivedState();
    }

    private void updateSysrootPackageDisplayText() {
        final var old = this.sysrootPackageDisplayText;
        if (isSysrootPackageSelected()) {
            final var pkg = getSysrootToolchains().get(selectedSysrootPackageIndex);
            final var ver = pkg.getVersions().get(selectedSysrootPackageVersionIndex);
            this.sysrootPackageDisplayText = String.format("%s(%s)", pkg.getName(), ver);
        } else {
            this.sysrootPackageDisplayText = "";
        }
        pcs.firePropertyChange("sysrootPackageDisplayText", old, this.sysrootPackageDisplayText);
    }

    /** Returns toolchains that can act as sysroot sources. */
    public List<Toolchain> getSysrootToolchains() {
        return toolchains.stream().filter(Toolchain::hasIncludedSysroot).toList();
    }

    /** Returns the configured venv parent directory. */
    public String getVenvLocation() {
        return venvLocation;
    }

    /** Sets the configured venv parent directory. */
    public void setVenvLocation(String location) {
        final var old = this.venvLocation;
        this.venvLocation = location == null ? "" : location;
        pcs.firePropertyChange("venvLocation", old, this.venvLocation);
    }

    /** Returns whether the venv parent directory can be edited by users. */
    public boolean isVenvLocationReadOnly() {
        return venvLocationReadOnly;
    }

    /** Sets whether the venv parent directory can be edited by users. */
    public void setVenvLocationReadOnly(boolean readOnly) {
        final var old = this.venvLocationReadOnly;
        this.venvLocationReadOnly = readOnly;
        pcs.firePropertyChange("venvLocationReadOnly", old, this.venvLocationReadOnly);
    }

    /** Returns the list of project root paths. */
    public IObservableList<String> getProjectRootPaths() {
        return projectRootPaths;
    }

    /** Replaces the list of project root paths. */
    public void setProjectRootPaths(Collection<String> paths) {
        final Runnable update = () -> {
            projectRootPaths.clear();
            if (paths != null) {
                for (final var path : paths) {
                    if (path == null || path.isBlank()) {
                        continue;
                    }
                    projectRootPaths.add(path);
                }
            }
            if (venvLocation.isBlank() && projectRootPaths.size() == 1) {
                setVenvLocation(projectRootPaths.get(0));
            }
        };
        if (projectRootPaths.getRealm().isCurrent()) {
            update.run();
        } else {
            projectRootPaths.getRealm().asyncExec(update);
        }
    }

    /** Returns the venv directory name. */
    public String getVenvName() {
        return venvName;
    }

    /** Sets the venv directory name. */
    public void setVenvName(String name) {
        setVenvNameInternal(name, true);
    }

    private void setVenvNameInternal(String name, boolean manualOverride) {
        final var normalizedName = name == null ? "" : name;
        if (normalizedName.equals(this.venvName)) {
            return;
        }
        if (manualOverride) {
            venvNameManuallyOverridden = true;
        }
        final var old = this.venvName;
        this.venvName = normalizedName;
        pcs.firePropertyChange("venvName", old, this.venvName);
    }

    /** Returns the summary text presented by the wizard. */
    public String getSummaryText() {
        return summaryText;
    }

    /** Returns whether the configuration page is complete. */
    public boolean isConfigurationPageComplete() {
        return configurationPageComplete;
    }

    /** Returns whether package data is currently being loaded. */
    public boolean isDataLoading() {
        return dataLoading;
    }

    /** Sets whether package data is currently being loaded. */
    public void setDataLoading(boolean loading) {
        final var old = this.dataLoading;
        this.dataLoading = loading;
        pcs.firePropertyChange("dataLoading", old, this.dataLoading);
    }

    /** Returns the current loading progress message. */
    public String getLoadingMessage() {
        return loadingMessage;
    }

    /** Sets the current loading progress message. */
    public void setLoadingMessage(String message) {
        final var old = this.loadingMessage;
        this.loadingMessage = message == null ? "" : message;
        pcs.firePropertyChange("loadingMessage", old, this.loadingMessage);
    }

    /** Returns the last loading error message, or an empty string if none. */
    public String getLoadingErrorMessage() {
        return loadingErrorMessage;
    }

    /** Sets the last loading error message. */
    public void setLoadingErrorMessage(String message) {
        final var old = this.loadingErrorMessage;
        this.loadingErrorMessage = message == null ? "" : message;
        pcs.firePropertyChange("loadingErrorMessage", old, this.loadingErrorMessage);
    }

    /** Returns whether asynchronous data loading has been started at least once. */
    public boolean isDataLoadStarted() {
        return dataLoadStarted;
    }

    /** Adds a property change listener. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /** Removes a property change listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
