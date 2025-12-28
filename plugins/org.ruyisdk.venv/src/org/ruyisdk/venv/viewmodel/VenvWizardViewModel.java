package org.ruyisdk.venv.viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.ruyisdk.ruyi.services.RuyiCli;
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
    private int selectedToolchainIndex = -1;
    private int selectedToolchainVersionIndex = -1;
    private final List<Emulator> emulators = new ArrayList<>();
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
        refreshListsBestEffort();
        recomputeDerivedState();
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
                sb.append(" (quirks: ").append(quirks).append(")");
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

    // TODO: remove try-catch
    private void refreshListsBestEffort() {
        try {
            profiles.clear();
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
                    toolchains.add(new Toolchain(toolchainInfo.getName(), toolchainInfo.getVersions()));
                }
            }

            final var emulatorInfos = service.listEmulators();
            if (emulatorInfos != null) {
                for (final var emulatorInfo : emulatorInfos) {
                    emulators.add(new Emulator(emulatorInfo.getName(), emulatorInfo.getVersions()));
                }
            }
        } catch (Exception ex) {
            // ignore errors; leave lists as-is
        }
    }

    // TODO: no returning RuyiCli.RunResult
    /** Updates the CLI index and refreshes view model data. */
    public RuyiCli.RunResult updateIndex() {
        final var result = service.updateIndex();
        refreshListsBestEffort();
        recomputeDerivedState();
        pcs.firePropertyChange("dataRefreshed", null, Boolean.TRUE);
        return result;
    }

    // TODO: no returning RuyiCli.RunResult
    private RuyiCli.RunResult installToolchain(String name, String version) {
        return service.installPackage(name, version);
    }

    // TODO: no returning RuyiCli.RunResult
    /** Installs the currently-selected toolchain package. */
    public RuyiCli.RunResult installToolchain() {
        if (selectedToolchainIndex < 0 || selectedToolchainIndex >= toolchains.size()
                        || selectedToolchainVersionIndex < 0) {
            return new RuyiCli.RunResult(-1, "No toolchain selected");
        }
        final var name = toolchains.get(selectedToolchainIndex).getName();
        final var version = toolchains.get(selectedToolchainIndex).getVersions().get(selectedToolchainVersionIndex);
        return installToolchain(name, version);
    }

    // TODO: no returning RuyiCli.RunResult
    private RuyiCli.RunResult installEmulator(String name, String version) {
        return service.installPackage(name, version);
    }

    // TODO: no returning RuyiCli.RunResult
    /** Installs the currently-selected emulator package when enabled. */
    public RuyiCli.RunResult installEmulator() {
        if (!emulatorEnabled) {
            return new RuyiCli.RunResult(0, "Emulator disabled");
        }
        if (selectedEmulatorIndex < 0 || selectedEmulatorIndex >= emulators.size()
                        || selectedEmulatorVersionIndex < 0) {
            return new RuyiCli.RunResult(-1, "Emulator enabled but not selected");
        }
        final var name = emulators.get(selectedEmulatorIndex).getName();
        final var version = emulators.get(selectedEmulatorIndex).getVersions().get(selectedEmulatorVersionIndex);
        return installEmulator(name, version);
    }

    // TODO: pass all arguments to service as-is
    // TODO: no returning RuyiCli.RunResult
    private RuyiCli.RunResult createVenv(String path, String toolchainName, String toolchainVersion, String profile,
                    String emulatorName, String emulatorVersion) {
        return service.createVenv(path, toolchainName, toolchainVersion, profile, emulatorName, emulatorVersion);
    }

    // TODO: eliminate path-related and validation things
    // TODO: no returning RuyiCli.RunResult
    /** Creates a virtual environment using the current wizard selections. */
    public RuyiCli.RunResult createVenv() {
        final var parent = this.venvLocation;
        if (parent == null || parent.isBlank()) {
            return new RuyiCli.RunResult(-1, "Venv parent path is empty");
        }
        final var name = this.venvName;
        if (name == null || name.isBlank()) {
            return new RuyiCli.RunResult(-1, "Venv name is empty");
        }

        if (selectedToolchainIndex < 0 || selectedToolchainIndex >= toolchains.size()
                        || selectedToolchainVersionIndex < 0) {
            return new RuyiCli.RunResult(-1, "No toolchain selected");
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
                return new RuyiCli.RunResult(-1, "Emulator enabled but not selected");
            }
            emulatorName = emulators.get(selectedEmulatorIndex).getName();
            emulatorVersion = emulators.get(selectedEmulatorIndex).getVersions().get(selectedEmulatorVersionIndex);
        }

        final var target = new File(parent, name);
        final var path = target.getPath();
        return createVenv(path, toolchainName, toolchainVersion, profile, emulatorName, emulatorVersion);
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
