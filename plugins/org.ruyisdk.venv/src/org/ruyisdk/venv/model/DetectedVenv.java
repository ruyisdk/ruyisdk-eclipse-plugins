package org.ruyisdk.venv.model;

public class DetectedVenv {
    private final String path;
    private final String profile;
    private final String sysroot;

    /**
     * Creates an instance.
     *
     * @param path venv path
     * @param profile associated profile name
     * @param sysroot sysroot path
     */
    public DetectedVenv(String path, String profile, String sysroot) {
        this.path = path;
        this.profile = profile;
        this.sysroot = sysroot;
    }

    public String getPath() {
        return path;
    }

    public String getProfile() {
        return profile;
    }

    public String getSysroot() {
        return sysroot;
    }
}
