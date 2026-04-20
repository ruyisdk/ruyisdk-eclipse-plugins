package org.ruyisdk.venv.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model representing a Ruyi virtual environment (venv). */
public class Venv {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String path;
    private String profile;
    private String sysroot;
    private String projectPath;
    private List<String> quirks;
    private String toolchainPath;
    private String toolchainPrefix;

    private Venv(String path, String profile, String sysroot, String projectPath,
            List<String> quirks) {
        this.path = path;
        this.profile = profile;
        this.sysroot = sysroot;
        this.projectPath = projectPath == null ? "" : projectPath;
        this.quirks = quirks == null ? new ArrayList<>() : new ArrayList<>(quirks);
        this.toolchainPath = "";
        this.toolchainPrefix = "";
    }

    /** Creates a venv model for a standalone venv with quirks. */
    public static Venv createStandalone(String path, String profile, String sysroot,
            List<String> quirks) {
        return new Venv(path, profile, sysroot, "", quirks);
    }

    /** Creates a venv model for a project venv. */
    public static Venv createForProject(String path, String profile, String sysroot,
            String projectPath) {
        return new Venv(path, profile, sysroot, projectPath, List.of());
    }

    /** Returns the venv path. */
    public String getPath() {
        return path;
    }

    /** Updates the venv path. */
    public void setPath(String path) {
        pcs.firePropertyChange("path", this.path, this.path = path);
    }

    /** Returns the profile name. */
    public String getProfile() {
        return profile;
    }

    /** Updates the profile name. */
    public void setProfile(String profile) {
        pcs.firePropertyChange("profile", this.profile, this.profile = profile);
    }

    /** Returns the sysroot path. */
    public String getSysroot() {
        return sysroot;
    }

    /** Updates the sysroot path. */
    public void setSysroot(String sysroot) {
        pcs.firePropertyChange("sysroot", this.sysroot, this.sysroot = sysroot);
    }

    /** Returns the associated project path, if any. */
    public String getProjectPath() {
        return projectPath;
    }

    /** Updates the associated project path. */
    public void setProjectPath(String projectPath) {
        pcs.firePropertyChange("projectPath", this.projectPath,
                this.projectPath = projectPath == null ? "" : projectPath);
    }

    /** Registers a listener for property changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /** Unregisters a listener for property changes. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /** Returns the quirks list. */
    public List<String> getQuirks() {
        return Collections.unmodifiableList(quirks);
    }

    /** Updates the quirks list. */
    public void setQuirks(List<String> quirks) {
        pcs.firePropertyChange("quirks", this.quirks,
                this.quirks = quirks == null ? new ArrayList<>() : new ArrayList<>(quirks));
    }

    /** Returns the toolchain bin directory path (derived from venv). */
    public String getToolchainPath() {
        return toolchainPath;
    }

    /** Updates the toolchain bin directory path. */
    public void setToolchainPath(String toolchainPath) {
        pcs.firePropertyChange("toolchainPath", this.toolchainPath,
                this.toolchainPath = toolchainPath == null ? "" : toolchainPath);
    }

    /** Returns the toolchain prefix (e.g., "riscv64-unknown-elf"). */
    public String getToolchainPrefix() {
        return toolchainPrefix;
    }

    /** Updates the toolchain prefix. */
    public void setToolchainPrefix(String toolchainPrefix) {
        pcs.firePropertyChange("toolchainPrefix", this.toolchainPrefix,
                this.toolchainPrefix = toolchainPrefix == null ? "" : toolchainPrefix);
    }
}
