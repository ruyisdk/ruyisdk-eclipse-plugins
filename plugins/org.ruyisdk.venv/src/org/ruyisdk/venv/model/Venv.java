package org.ruyisdk.venv.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Model representing a Ruyi virtual environment (venv). */
public class Venv {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private String path;
    private String profile;
    private String sysroot;
    private String projectPath;
    private String quirks;
    private Boolean activated;

    /** Creates a venv model for a standalone venv. */
    public Venv(String path, String profile, String sysroot, Boolean activated) {
        this(path, profile, sysroot, activated, "");
    }

    /** Creates a venv model for a standalone venv with quirks. */
    public Venv(String path, String profile, String sysroot, Boolean activated, String quirks) {
        this.path = path;
        this.profile = profile;
        this.sysroot = sysroot;
        this.projectPath = "";
        this.activated = activated;
        this.quirks = quirks;
    }

    /** Creates a venv model for a project venv. */
    public Venv(String path, String profile, String sysroot, String projectPath) {
        this.path = path;
        this.profile = profile;
        this.sysroot = sysroot;
        this.projectPath = projectPath == null ? "" : projectPath;
        this.activated = null;
        this.quirks = "";
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

    /** Returns whether the venv is activated. */
    public Boolean getActivated() {
        return activated;
    }

    /** Updates whether the venv is activated. */
    public void setActivated(Boolean activated) {
        pcs.firePropertyChange("activated", this.activated, this.activated = activated);
    }

    /** Registers a listener for property changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /** Unregisters a listener for property changes. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /** Returns the quirks string. */
    public String getQuirks() {
        return quirks;
    }

    /** Updates the quirks string. */
    public void setQuirks(String quirks) {
        pcs.firePropertyChange("quirks", this.quirks, this.quirks = quirks);
    }
}
