package org.ruyisdk.venv.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model representing a Ruyi profile. */
public class Profile {
    private String name;
    private List<String> quirks = new ArrayList<>();

    /** Creates a profile model. */
    public Profile(String name, List<String> quirks) {
        this.name = name;
        if (quirks != null) {
            this.quirks.addAll(quirks);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getQuirks() {
        return Collections.unmodifiableList(quirks);
    }

    /**
     * Sets the quirks (flavors) provided by this profile.
     *
     * @param quirks the new quirks list
     */
    public void setQuirks(List<String> quirks) {
        this.quirks = new ArrayList<>();
        if (quirks != null) {
            this.quirks.addAll(quirks);
        }
    }
}
