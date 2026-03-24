package org.ruyisdk.venv.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model representing a toolchain and its available versions. */
public class Toolchain {
    private String name;
    private List<String> versions = new ArrayList<>();
    private List<String> quirks = new ArrayList<>();

    /** Creates a toolchain with the given versions and quirks. */
    public Toolchain(String name, List<String> versions, List<String> quirks) {
        this.name = name;
        if (versions != null) {
            this.versions.addAll(versions);
        }
        if (quirks != null) {
            this.quirks.addAll(quirks);
        }
    }

    /** Returns the toolchain name. */
    public String getName() {
        return name;
    }

    /** Updates the toolchain name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns an unmodifiable view of toolchain versions. */
    public List<String> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    /** Replaces the available toolchain versions. */
    public void setVersions(List<String> versions) {
        this.versions = new ArrayList<>();
        if (versions != null) {
            this.versions.addAll(versions);
        }
    }

    /** Returns the quirks (flavors) provided by this toolchain. */
    public List<String> getQuirks() {
        return Collections.unmodifiableList(quirks);
    }

    /** Sets the quirks (flavors) provided by this toolchain. */
    public void setQuirks(List<String> quirks) {
        this.quirks = new ArrayList<>();
        if (quirks != null) {
            this.quirks.addAll(quirks);
        }
    }
}
