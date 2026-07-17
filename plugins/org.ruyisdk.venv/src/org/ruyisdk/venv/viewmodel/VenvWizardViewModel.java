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
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
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

    private final List<Profile> profiles = new ArrayList<>();
    private int selectedProfileIndex = -1;

    private final List<Toolchain> toolchains = new ArrayList<>();
    private final List<Toolchain> allToolchains = new ArrayList<>();
    private int selectedToolchainIndex = -1;
    private int selectedToolchainVersionIndex = -1;

    private final List<Emulator> emulators = new ArrayList<>();
    private final List<Emulator> allEmulators = new ArrayList<>();
    private int selectedEmulatorIndex = -1;
    private int selectedEmulatorVersionIndex = -1;
    private boolean emulatorEnabled = false;

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
                        && toolchains.get(selectedToolchainIndex).hasIncludedSysroot();;
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

    private void loadLists() {
        profiles.clear();
        allToolchains.clear();
        allEmulators.clear();
        toolchains.clear();
        emulators.clear();

        final var profileInfos = service.listProfiles();
        if (profileInfos != null) {
            for (final var profileInfo : profileInfos) {
                profiles.add(new Profile(profileInfo.getName(), profileInfo.getQuirks()));
            }
        }

        final var toolchainInfos = service.listToolchains();
        if (toolchainInfos != null) {
            for (final var toolchainInfo : toolchainInfos) {
                allToolchains
                        .add(new Toolchain(toolchainInfo.getName(), toolchainInfo.getVersions(),
                                toolchainInfo.getQuirks(), toolchainInfo.hasIncludedSysroot()));
            }
        }

        final var emulatorInfos = service.listEmulators();
        if (emulatorInfos != null) {
            for (final var emulatorInfo : emulatorInfos) {
                allEmulators.add(new Emulator(emulatorInfo.getName(), emulatorInfo.getVersions(),
                        emulatorInfo.getQuirks()));
            }
        }
    }

    /**
     * Filters toolchains and emulators to match the selected profile's quirks. When no profile is
     * selected, all packages are shown.
     */
    private void filterPackagesBySelectedProfile() {
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

        // Reset selections since the lists changed
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

    /** Loads package lists from the Ruyi CLI and refreshes all view model data. */
    public void loadAll() {
        loadLists();
        filterPackagesBySelectedProfile();
        recomputeDerivedState();
    }

    private void installToolchain(String name, String version) {
        service.installPackage(name, version);
    }

    /** Installs the currently-selected toolchain package. */
    public void installToolchain() {
        if (selectedToolchainIndex < 0 || selectedToolchainIndex >= toolchains.size()
                || selectedToolchainVersionIndex < 0) {
            throw RuyiCliException.invalidArgument("No toolchain selected");
        }
        final var name = toolchains.get(selectedToolchainIndex).getName();
        final var version = toolchains.get(selectedToolchainIndex).getVersions()
                .get(selectedToolchainVersionIndex);
        installToolchain(name, version);
    }

    private void installEmulator(String name, String version) {
        service.installPackage(name, version);
    }

    /** Installs the currently-selected emulator package when enabled. */
    public void installEmulator() {
        if (!emulatorEnabled) {
            throw RuyiCliException.invalidArgument("Emulator disabled");
        }
        if (selectedEmulatorIndex < 0 || selectedEmulatorIndex >= emulators.size()
                || selectedEmulatorVersionIndex < 0) {
            throw RuyiCliException.invalidArgument("Emulator enabled but not selected");
        }
        final var name = emulators.get(selectedEmulatorIndex).getName();
        final var version = emulators.get(selectedEmulatorIndex).getVersions()
                .get(selectedEmulatorVersionIndex);
        installEmulator(name, version);
    }

    private void installPackageForSysroot(String name, String version) {
        service.installPackage(name, version);
    }

    /** Installs the currently-selected sysroot package when enabled. */
    public void installPackageForSysroot() {
        if (sysrootOption != SysrootOption.FOREIGN_TOOLCHAIN) {
            throw RuyiCliException.invalidArgument("No need to install package for sysroot");
        }
        final var sysrootToolchains = getSysrootToolchains();
        if (selectedSysrootPackageIndex < 0
                || selectedSysrootPackageIndex >= sysrootToolchains.size()
                || selectedSysrootPackageVersionIndex < 0) {
            throw RuyiCliException.invalidArgument("Sysroot enabled but not selected");
        }
        final var pkg = sysrootToolchains.get(selectedSysrootPackageIndex);
        final var name = pkg.getName();
        final var version = pkg.getVersions().get(selectedSysrootPackageVersionIndex);
        installPackageForSysroot(name, version);
    }

    private void createVenv(String path, String toolchainName, String toolchainVersion,
            String profile, Boolean withSysroot, String sysrootFrom, String copySysrootFromDir,
            String symlinkSysrootFromDir, String projectSysrootFromRootfs, String emulatorName,
            String emulatorVersion) {
        service.createVenv(path, toolchainName, toolchainVersion, profile, withSysroot, sysrootFrom,
                copySysrootFromDir, symlinkSysrootFromDir, projectSysrootFromRootfs, emulatorName,
                emulatorVersion);
    }

    /** Creates a virtual environment using the current wizard selections. */
    public void createVenv() {
        final var parent = this.venvLocation;
        if (parent == null || parent.isBlank()) {
            throw RuyiCliException.invalidArgument("Venv parent path is empty");
        }
        final var name = this.venvName;
        if (name == null || name.isBlank()) {
            throw RuyiCliException.invalidArgument("Venv name is empty");
        }

        if (selectedToolchainIndex < 0 || selectedToolchainIndex >= toolchains.size()
                || selectedToolchainVersionIndex < 0) {
            throw RuyiCliException.invalidArgument("No toolchain selected");
        }
        final var toolchainName = toolchains.get(selectedToolchainIndex).getName();
        final var toolchainVersion = toolchains.get(selectedToolchainIndex).getVersions()
                .get(selectedToolchainVersionIndex);

        String profile = null;
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            profile = profiles.get(selectedProfileIndex).getName();
        }

        Boolean withSysroot = null;
        String sysrootFromAtom = null;
        String copySysrootFromDir = null;
        String symlinkSysrootFromDir = null;
        String projectSysrootFromRootfs = null;
        if (this.sysrootOption == SysrootOption.DEFAULT_SYSROOT) {
            withSysroot = true;
        } else if (this.sysrootOption == SysrootOption.NONE_SYSROOT) {
            withSysroot = false;
        } else if (this.sysrootOption == SysrootOption.FOREIGN_TOOLCHAIN) {
            if (!isSysrootPackageSelected()) {
                throw RuyiCliException
                        .invalidArgument("Sysroot from package selected but no package chosen");
            }
            final var pkg = getSysrootToolchains().get(selectedSysrootPackageIndex);
            final var ver = pkg.getVersions().get(selectedSysrootPackageVersionIndex);
            sysrootFromAtom = String.format("%s(%s)", pkg.getName(), ver);
        } else if (this.sysrootOption == SysrootOption.COPY_FROM_DIRECTORY) {
            if (sysrootDirectoryPath == null || sysrootDirectoryPath.isBlank()) {
                throw RuyiCliException
                        .invalidArgument("Copy sysroot from directory selected but no path set");
            }
            copySysrootFromDir = sysrootDirectoryPath;
        } else if (this.sysrootOption == SysrootOption.SYMLINK_FROM_DIRECTORY) {
            if (sysrootDirectoryPath == null || sysrootDirectoryPath.isBlank()) {
                throw RuyiCliException
                        .invalidArgument("Symlink sysroot from directory selected but no path set");
            }
            symlinkSysrootFromDir = sysrootDirectoryPath;
        } else if (this.sysrootOption == SysrootOption.PROJECT_FROM_ROOTFS) {
            if (sysrootDirectoryPath == null || sysrootDirectoryPath.isBlank()) {
                throw RuyiCliException
                        .invalidArgument("Project sysroot from rootfs selected but no path set");
            }
            projectSysrootFromRootfs = sysrootDirectoryPath;
        }

        String emulatorName = null;
        String emulatorVersion = null;
        if (emulatorEnabled) {
            if (selectedEmulatorIndex < 0 || selectedEmulatorIndex >= emulators.size()
                    || selectedEmulatorVersionIndex < 0) {
                throw RuyiCliException.invalidArgument("Emulator enabled but not selected");
            }
            emulatorName = emulators.get(selectedEmulatorIndex).getName();
            emulatorVersion = emulators.get(selectedEmulatorIndex).getVersions()
                    .get(selectedEmulatorVersionIndex);
        }

        final var target = new File(parent, name);
        final var path = target.getPath();
        createVenv(path, toolchainName, toolchainVersion, profile, withSysroot, sysrootFromAtom,
                copySysrootFromDir, symlinkSysrootFromDir, projectSysrootFromRootfs, emulatorName,
                emulatorVersion);
    }

    /** Returns available profiles. */
    public List<Profile> getProfiles() {
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
        filterPackagesBySelectedProfile();
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

    /** Returns available toolchains. */
    public List<Toolchain> getToolchains() {
        return toolchains;
    }

    /** Returns the selected toolchain index. */
    public int getSelectedToolchainIndex() {
        return selectedToolchainIndex;
    }

    /** Sets the selected toolchain index. */
    public void setSelectedToolchainIndex(int index) {
        final var old = this.selectedToolchainIndex;
        this.selectedToolchainIndex = index;
        pcs.firePropertyChange("selectedToolchainIndex", old, this.selectedToolchainIndex);
        if (old != index) {
            setSelectedToolchainVersionIndex(-1);
        }
        recomputeDerivedState();
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

    /** Returns available emulators. */
    public List<Emulator> getEmulators() {
        return emulators;
    }

    /** Returns the selected emulator index. */
    public int getSelectedEmulatorIndex() {
        return selectedEmulatorIndex;
    }

    /** Sets the selected emulator index. */
    public void setSelectedEmulatorIndex(int index) {
        final var old = this.selectedEmulatorIndex;
        this.selectedEmulatorIndex = index;
        pcs.firePropertyChange("selectedEmulatorIndex", old, this.selectedEmulatorIndex);
        if (old != index) {
            setSelectedEmulatorVersionIndex(-1);
        }
        recomputeDerivedState();
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

    /** Adds a property change listener. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /** Removes a property change listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
