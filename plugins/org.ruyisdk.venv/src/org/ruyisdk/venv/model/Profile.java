package org.ruyisdk.venv.model;

/** Model representing a Ruyi profile. */
public class Profile {
    private String name;
    private String quirks;

    /** Creates a profile model. */
    public Profile(String name, String quirks) {
        this.name = name;
        this.quirks = quirks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuirks() {
        return quirks;
    }

    public void setQuirks(String quirks) {
        this.quirks = quirks;
    }
}
