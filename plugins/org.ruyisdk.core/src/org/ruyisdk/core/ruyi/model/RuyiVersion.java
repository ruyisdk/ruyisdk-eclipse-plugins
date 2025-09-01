package org.ruyisdk.core.ruyi.model;

/**
 * Represents a semantic version number for Ruyi following MAJOR.MINOR.PATCH format.
 *
 * <p>This immutable class implements semantic versioning comparison logic and provides
 * parsing capabilities from version strings. Used for version checking and upgrade
 * decisions throughout RuyiSDK.
 */
public class RuyiVersion implements Comparable<RuyiVersion> {
    /** Major version number (incremented for incompatible API changes). */
    private final int major;
    
    /** Minor version number (incremented for backward-compatible functionality). */
    private final int minor;
    
    /** Patch version number (incremented for backward-compatible bug fixes). */
    private final int patch;

    /**
     * Constructs a new version instance.
     *
     * @param major major version number
     * @param minor minor version number
     * @param patch patch version number
     */
    public RuyiVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    
    /**
     * Parses a version string in "MAJOR.MINOR.PATCH" format.
     *
     * @param versionStr the version string to parse
     * @return parsed RuyiVersion instance, or null if format is invalid
     */
    public static RuyiVersion parse(String versionStr) {
        try {
            String[] parts = versionStr.split("\\.");
            if (parts.length != 3) {
                System.out.print("Invalid version format: " + versionStr);
                return null;
            }
            return new RuyiVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            System.out.print("Invalid version number: " + versionStr);
            return null;
        }
    }

    /**
     * Compares this version with another following semantic versioning rules.
     *
     * @param other the version to compare with
     * @return negative if this is older, positive if newer, zero if equal
     */
    @Override
    public int compareTo(RuyiVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }
}
