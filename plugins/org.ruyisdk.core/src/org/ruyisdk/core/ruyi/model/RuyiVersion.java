package org.ruyisdk.core.ruyi.model;

/**
 * Ruyi 版本模型
 */
public class RuyiVersion implements Comparable<RuyiVersion> {
    private final int major;
    private final int minor;
    private final int patch;

    public RuyiVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

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

    // Getters
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
