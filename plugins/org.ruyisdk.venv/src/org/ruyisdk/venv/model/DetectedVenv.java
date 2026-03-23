package org.ruyisdk.venv.model;

/**
 * Immutable data holder representing a virtual environment discovered on the filesystem.
 *
 * <p>
 * Instances are created by the detection/scanning logic in {@link VenvDetectionService} and later
 * enriched into full {@link Venv} models.
 */
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

    /** Returns the absolute path of the detected venv directory. */
    public String getPath() {
        return path;
    }

    /** Returns the profile name associated with this venv. */
    public String getProfile() {
        return profile;
    }

    /** Returns the sysroot path for this venv. */
    public String getSysroot() {
        return sysroot;
    }
}
