package org.ruyisdk.venv.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
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

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final VenvDetectionService service;

    private boolean configurationPageComplete;
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
    private String venvLocation = "";
    private String venvName = "";
    private final IObservableList<String> projectRootPaths = new WritableList<>(new ArrayList<>(), String.class);

    /** Available sysroot selection strategies. */
    public enum SysrootOption {
        NONE_SYSROOT, DEFAULT_SYSROOT, FOREIGN_TOOLCHAIN
    }

    /** Creates a new view model instance. */
    public VenvWizardViewModel(VenvDetectionService service) {
        this.service = service;
    }

    private void recomputeDerivedState() {
        updateSummaryText();
        recomputeConfigurationPageComplete();
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

        sb.append("Sysroot: ").append(sysrootOption.toString());
        return sb.toString();
    }

    private void refreshLists() {
        service.updateIndex();

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
                allToolchains.add(new Toolchain(toolchainInfo.getName(), toolchainInfo.getVersions(),
                                toolchainInfo.getQuirks()));
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
        setSelectedEmulatorIndex(-1);
    }

    private static boolean quirksMatch(List<String> profileQuirks, List<String> packageQuirks) {
        final var neededByProfile = profileQuirks == null ? Set.<String>of() : new HashSet<>(profileQuirks);
        final var providedByPackage = packageQuirks == null ? Set.<String>of() : new HashSet<>(packageQuirks);
        if (neededByProfile.isEmpty()) {
            return providedByPackage.isEmpty();
        }
        return providedByPackage.containsAll(neededByProfile);
    }

    /** Updates the CLI index and refreshes all view model data. */
    public void refreshAll() {
        refreshLists();
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
        final var version = toolchains.get(selectedToolchainIndex).getVersions().get(selectedToolchainVersionIndex);
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
        final var version = emulators.get(selectedEmulatorIndex).getVersions().get(selectedEmulatorVersionIndex);
        installEmulator(name, version);
    }

    private void createVenv(String path, String toolchainName, String toolchainVersion, String profile,
                    String emulatorName, String emulatorVersion) {
        service.createVenv(path, toolchainName, toolchainVersion, profile, emulatorName, emulatorVersion);
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
        final var toolchainVersion =
                        toolchains.get(selectedToolchainIndex).getVersions().get(selectedToolchainVersionIndex);

        String profile = null;
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            profile = profiles.get(selectedProfileIndex).getName();
        }

        String emulatorName = null;
        String emulatorVersion = null;
        if (emulatorEnabled) {
            if (selectedEmulatorIndex < 0 || selectedEmulatorIndex >= emulators.size()
                            || selectedEmulatorVersionIndex < 0) {
                throw RuyiCliException.invalidArgument("Emulator enabled but not selected");
            }
            emulatorName = emulators.get(selectedEmulatorIndex).getName();
            emulatorVersion = emulators.get(selectedEmulatorIndex).getVersions().get(selectedEmulatorVersionIndex);
        }

        final var target = new File(parent, name);
        final var path = target.getPath();
        createVenv(path, toolchainName, toolchainVersion, profile, emulatorName, emulatorVersion);
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
        filterPackagesBySelectedProfile();
        recomputeDerivedState();
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
        pcs.firePropertyChange("selectedToolchainVersionIndex", old, this.selectedToolchainVersionIndex);
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
        pcs.firePropertyChange("selectedEmulatorVersionIndex", old, this.selectedEmulatorVersionIndex);
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

    /** Sets the sysroot selection option. */
    public void setSysrootOption(SysrootOption option) {
        final var old = this.sysrootOption;
        this.sysrootOption = option;
        pcs.firePropertyChange("sysrootOption", old, this.sysrootOption);
        recomputeDerivedState();
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
        final var old = this.venvName;
        this.venvName = name == null ? "" : name;
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
