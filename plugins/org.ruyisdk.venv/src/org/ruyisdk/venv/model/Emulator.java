package org.ruyisdk.venv.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An emulator definition with a name and available versions.
 */
public class Emulator {
    private String name;
    private List<String> versions = new ArrayList<>();

    /**
     * Creates an emulator.
     *
     * @param name the emulator name
     * @param versions the available versions
     */
    public Emulator(String name, List<String> versions) {
        this.name = name;
        if (versions != null) {
            this.versions.addAll(versions);
        }
    }

    /**
     * Returns the emulator name.
     *
     * @return the emulator name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the emulator name.
     *
     * @param name the emulator name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the available versions.
     *
     * @return an unmodifiable list of versions
     */
    public List<String> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    /**
     * Sets the available versions.
     *
     * @param versions the available versions
     */
    public void setVersions(List<String> versions) {
        this.versions = new ArrayList<>();
        if (versions != null) {
            this.versions.addAll(versions);
        }
    }
}
