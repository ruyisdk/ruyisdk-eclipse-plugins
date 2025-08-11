package org.ruyisdk.core.ruyi.model;

/**
 * 版本信息值对象
 */
public class RuyiReleaseInfo {
    private final RuyiVersion version;
    private final String channel;
    private final String filename;
    private final String githubUrl;
    private final String mirrorUrl;

    public RuyiReleaseInfo(RuyiVersion version, String channel, String filename, String githubUrl, String mirrorUrl) {
        this.version = version;
        this.channel = channel;
        this.filename = filename;
        this.githubUrl = githubUrl;
        this.mirrorUrl = mirrorUrl;
    }

    // Getter 方法
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
