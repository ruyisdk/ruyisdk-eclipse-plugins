package org.ruyisdk.core.ruyi.model;

/**
 * Value object representing Ruyi version release information.
 *
 * <p>Contains metadata about a specific RuyiSDK release including version number,
 * release channel, distribution file information, and download URLs.
 */
public class RuyiReleaseInfo {
    /** The version number of this release. */
    private final RuyiVersion version;
    
    /** 
     * The release channel. 
     * Determines update frequency and stability level.
     */
    private final String channel;
    
    /** 
     * The filename of the distribution package.
     * Typically includes version and platform information.
     */
    private final String filename;
    
    /** 
     * The GitHub download URL for this release.
     * Primary download source for most users.
     */
    private final String githubUrl;
    
    /** 
     * The mirror download URL for this release.
     * Alternative download source, often faster in certain regions.
     */
    private final String mirrorUrl;

    /**
     * Constructs a new RuyiReleaseInfo instance.
     *
     * @param version the version number
     * @param channel the release channel
     * @param filename the distribution filename
     * @param githubUrl the GitHub download URL
     * @param mirrorUrl the mirror download URL
     */
    public RuyiReleaseInfo(RuyiVersion version, String channel, String filename, 
                          String githubUrl, String mirrorUrl) {
        this.version = version;
        this.channel = channel;
        this.filename = filename;
        this.githubUrl = githubUrl;
        this.mirrorUrl = mirrorUrl;
    }

    public RuyiVersion getVersion() {
        return version;
    }

    public String getChannel() {
        return channel;
    }

    public String getFilename() {
        return filename;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getMirrorUrl() {
        return mirrorUrl;
    }

    @Override
    public String toString() {
        return String.format("VersionInfo[version=%s, channel=%s, filename=%s, githubUrl=%s, mirrorUrl=%s]",
                        version.toString(), channel, filename, githubUrl, mirrorUrl);
    }
}