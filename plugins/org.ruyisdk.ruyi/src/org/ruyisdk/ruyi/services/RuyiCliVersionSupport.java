package org.ruyisdk.ruyi.services;

import java.util.regex.Pattern;
import org.ruyisdk.core.ruyi.model.RuyiVersion;

/**
 * Shared helpers for detecting and parsing the installed ruyi CLI version. Does not rely on
 * {@link RuyiCli} or {@link RuyiCliRequest}, but {@link RuyiCliExecutor}.
 */
public final class RuyiCliVersionSupport {
    private static final RuyiVersion MIN_SUPPORTED_VERSION = new RuyiVersion(0, 47, 0);
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(?m)^Ruyi\\s+(\\d+\\.\\d+\\.\\d+)\\b");

    private RuyiCliVersionSupport() {}

    /**
     * Detects installed ruyi version by executing {@code ruyi -V} with bypassing the version check
     * in {@link RuyiCliRequest}.
     *
     * @param installDir directory containing the ruyi binary
     * @return parsed semantic version, or {@code null} when output format is unsupported
     */
    public static RuyiVersion getInstalledVersion(String installDir) {
        final var result = RuyiCliExecutor.execute(installDir, null, null, null, null, 5, "-V");
        if (result.getExitCode() != 0) {
            throw RuyiCliException.executionFailed("ruyi -V", result.getExitCode(),
                    result.getOutput());
        }
        return parseVersionText(result.getOutput());
    }

    /**
     * Parses semantic version text from ruyi version command output.
     *
     * @param output full command output
     * @return parsed version or {@code null} when output format is unsupported
     */
    public static RuyiVersion parseVersionText(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }

        final var matcher = VERSION_PATTERN.matcher(output);
        if (!matcher.find()) {
            return null;
        }
        return RuyiVersion.parse(matcher.group(1));
    }

    /**
     * Checks whether a parsed ruyi version is supported by this plugin.
     *
     * @param version parsed installed version
     * @return true when version is 0.47.0 or newer, false otherwise
     */
    public static boolean isSupportedVersion(RuyiVersion version) {
        return version != null && version.compareTo(MIN_SUPPORTED_VERSION) >= 0;
    }

    /**
     * Returns the minimum supported ruyi version.
     *
     * @return minimum supported version
     */
    public static RuyiVersion getMinimumSupportedVersion() {
        return MIN_SUPPORTED_VERSION;
    }

    /**
     * Validates that a parsed ruyi version is 0.47.0 or newer.
     *
     * @param version parsed installed version
     * @throws RuyiCliException when version is unsupported
     */
    public static void ensureSupportedVersion(RuyiVersion version) {
        if (isSupportedVersion(version)) {
            return;
        }
        final var displayVersion = version == null ? "unknown" : version.toString();
        throw RuyiCliException.unsupportedVersion(MIN_SUPPORTED_VERSION.toString(), displayVersion);
    }
}
