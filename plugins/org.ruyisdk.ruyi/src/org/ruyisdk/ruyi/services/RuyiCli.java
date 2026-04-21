package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.ruyisdk.ruyi.model.TelemetryMode;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Simple DTOs returned by the CLI wrapper to avoid cross-plugin model coupling.
 */
public class RuyiCli {
    /** Profile information returned by the ruyi CLI. */
    public static class ProfileInfo {
        private final String name;
        private final List<String> quirks;

        /**
         * Creates an instance.
         *
         * @param name profile name
         * @param quirks quirk identifiers needed by this profile
         */
        public ProfileInfo(String name, List<String> quirks) {
            this.name = name;
            this.quirks = quirks == null ? new ArrayList<>() : new ArrayList<>(quirks);
        }

        public String getName() {
            return name;
        }

        public List<String> getQuirks() {
            return Collections.unmodifiableList(quirks);
        }
    }

    /** Toolchain package information returned by the ruyi CLI. */
    public static class ToolchainInfo {
        private final String name;
        private final List<String> versions;
        // "flavors" is the old name for "quirks":
        // https://github.com/ruyisdk/ruyi/blob/ff18bb37e092d875e04ddcb3320c79798c4b2315/ruyi/ruyipkg/pkg_manifest.py#L88-L109
        private final List<String> quirks;

        /**
         * Creates an instance.
         *
         * @param name toolchain package name
         * @param versions available toolchain versions
         * @param quirks quirk (flavor) identifiers provided by this toolchain
         */
        public ToolchainInfo(String name, List<String> versions, List<String> quirks) {
            this.name = name;
            this.versions = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
            this.quirks = quirks == null ? new ArrayList<>() : new ArrayList<>(quirks);
        }

        public String getName() {
            return name;
        }

        public List<String> getVersions() {
            return Collections.unmodifiableList(versions);
        }

        public List<String> getQuirks() {
            return Collections.unmodifiableList(quirks);
        }
    }

    /** Emulator package information returned by the ruyi CLI. */
    public static class EmulatorInfo {
        private final String name;
        private final List<String> versions;
        // "flavors" is the old name for "quirks":
        // https://github.com/ruyisdk/ruyi/blob/ff18bb37e092d875e04ddcb3320c79798c4b2315/ruyi/ruyipkg/pkg_manifest.py#L88-L109
        private final List<String> quirks;

        /**
         * Creates an instance.
         *
         * @param name emulator package name
         * @param versions available emulator versions
         * @param quirks quirk (flavor) identifiers provided by this emulator
         */
        public EmulatorInfo(String name, List<String> versions, List<String> quirks) {
            this.name = name;
            this.versions = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
            this.quirks = quirks == null ? new ArrayList<>() : new ArrayList<>(quirks);
        }

        public String getName() {
            return name;
        }

        public List<String> getVersions() {
            return Collections.unmodifiableList(versions);
        }

        public List<String> getQuirks() {
            return Collections.unmodifiableList(quirks);
        }
    }

    /** Package version information returned by package list porcelain output. */
    public static class PackageVersionInfo {
        private final String semver;
        private final String remark;
        private final boolean installed;

        /**
         * Creates an instance.
         *
         * @param semver package semantic version
         * @param remark optional remark associated with this version
         * @param installed true if this version is installed
         */
        public PackageVersionInfo(String semver, String remark, boolean installed) {
            this.semver = semver;
            this.remark = remark;
            this.installed = installed;
        }

        public String getSemver() {
            return semver;
        }

        public String getRemark() {
            return remark;
        }

        public boolean isInstalled() {
            return installed;
        }
    }

    /** Package list entry information returned by package list porcelain output. */
    public static class PackageListEntryInfo {
        private final String category;
        private final String name;
        private final List<PackageVersionInfo> versions;

        /**
         * Creates an instance.
         *
         * @param category package category
         * @param name package name
         * @param versions available package versions
         */
        public PackageListEntryInfo(String category, String name,
                List<PackageVersionInfo> versions) {
            this.category = category;
            this.name = name;
            this.versions = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
        }

        public String getCategory() {
            return category;
        }

        public String getName() {
            return name;
        }

        public List<PackageVersionInfo> getVersions() {
            return Collections.unmodifiableList(versions);
        }
    }

    /** Category node information for package tree rendering. */
    public static class PackageTreeCategoryInfo {
        private final String name;
        private final List<PackageTreePackageInfo> packages;

        /**
         * Creates an instance.
         *
         * @param name category name
         * @param packages packages in this category
         */
        public PackageTreeCategoryInfo(String name, List<PackageTreePackageInfo> packages) {
            this.name = name;
            this.packages = packages == null ? new ArrayList<>() : new ArrayList<>(packages);
        }

        public String getName() {
            return name;
        }

        public List<PackageTreePackageInfo> getPackages() {
            return Collections.unmodifiableList(packages);
        }
    }

    /** Package node information for package tree rendering. */
    public static class PackageTreePackageInfo {
        private final String name;
        private final List<PackageTreeVersionInfo> versions;

        /**
         * Creates an instance.
         *
         * @param name package name
         * @param versions versions under this package
         */
        public PackageTreePackageInfo(String name, List<PackageTreeVersionInfo> versions) {
            this.name = name;
            this.versions = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
        }

        public String getName() {
            return name;
        }

        public List<PackageTreeVersionInfo> getVersions() {
            return Collections.unmodifiableList(versions);
        }
    }

    /** Version node information for package tree rendering. */
    public static class PackageTreeVersionInfo {
        private final String displayName;
        private final String packageRef;
        private final boolean installed;

        /**
         * Creates an instance.
         *
         * @param displayName display name shown in the tree
         * @param packageRef package atom reference used by CLI actions
         * @param installed true when this version is already installed
         */
        public PackageTreeVersionInfo(String displayName, String packageRef, boolean installed) {
            this.displayName = displayName;
            this.packageRef = packageRef;
            this.installed = installed;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPackageRef() {
            return packageRef;
        }

        public boolean isInstalled() {
            return installed;
        }
    }

    /** Summary information for a news item returned by the ruyi CLI. */
    public static class NewsListItemInfo {
        private final String id;
        private final Integer ord;
        private final Boolean read;
        private final String title;

        /**
         * Creates an instance.
         *
         * @param id news item identifier
         * @param ord news item ordinal
         * @param read whether the item has been read
         * @param title news item title
         */
        public NewsListItemInfo(String id, Integer ord, Boolean read, String title) {
            this.id = id;
            this.ord = ord;
            this.read = read;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public Integer getOrd() {
            return ord;
        }

        public Boolean isRead() {
            return read;
        }

        public String getTitle() {
            return title;
        }
    }

    /** Full content and metadata for a news item returned by the ruyi CLI. */
    public static class NewsReadResult {
        private final String id;
        private final Integer ord;
        private final Boolean read;
        private final String title;
        private final String content;

        /**
         * Creates an instance.
         *
         * @param id news item identifier
         * @param ord news item ordinal
         * @param read whether the item has been read
         * @param title news item title
         * @param content news item content
         */
        public NewsReadResult(String id, Integer ord, Boolean read, String title, String content) {
            this.id = id;
            this.ord = ord;
            this.read = read;
            this.title = title;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public Integer getOrd() {
            return ord;
        }

        public Boolean isRead() {
            return read;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

    /** Lists available profiles as reported by the ruyi CLI. */
    public static List<ProfileInfo> listProfiles() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).list().profiles().end().build();
        final var result = request.execute();
        return parseProfilesFromString(result.getOutput());
    }

    /**
     * Parses profile list output.
     *
     * @param input raw profile list output
     * @return parsed profile entries
     */
    public static List<ProfileInfo> parseProfilesFromString(String input) {
        return RuyiCliParsingSupport.parseProfilesFromString(input);
    }

    /** Lists available news items using the ruyi CLI. */
    public static List<NewsListItemInfo> listNewsItems(boolean onlyUnread) {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).news().list(onlyUnread).end().build();
        final var result = request.execute();
        return parseNewsListFromString(result.getOutput());
    }

    /**
     * Reads a news item by ID or ordinal using the ruyi CLI.
     *
     * @param idOrOrdinal news item ID or ordinal
     * @return the news read result
     */
    public static NewsReadResult readNewsItem(String idOrOrdinal) {
        if (idOrOrdinal == null || idOrOrdinal.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid news item ID or ordinal");
        }
        // TODO: the 3-second timeout help us avoid hanging if the CLI gets stuck trying to read
        // news content.
        // The hang is due to a unresolved bug.
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).timeoutSeconds(3).news().read(idOrOrdinal).end().build();
        final var result = request.execute();
        return parseNewsReadFromString(result.getOutput());
    }

    /**
     * Parses news list output.
     *
     * @param input raw news list output
     * @return parsed news list items
     */
    public static List<NewsListItemInfo> parseNewsListFromString(String input) {
        return RuyiCliParsingSupport.parseNewsListFromString(input);
    }

    /**
     * Parses news read output.
     *
     * @param input raw news read output
     * @return parsed news read result, or null if no item could be parsed
     */
    public static NewsReadResult parseNewsReadFromString(String input) {
        return RuyiCliParsingSupport.parseNewsReadFromString(input);
    }

    private static String requireInstallPathResult() {
        return RuyiFileUtils.findInstallPathWithRuyi();
    }

    /**
     * Runs a command with experimental environment and optional working directory.
     *
     * @param args arguments without the `ruyi` executable
     * @param workingDirectory working directory for the process (may be {@code null})
     * @return command result with exit code and captured output
     */
    public static String runRuyiExperimental(List<String> args, File workingDirectory) {
        final var request =
                RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                        .args(args).experimental(true).workingDirectory(workingDirectory).build();
        return request.execute().getOutput();
    }

    // TODO: move to RuyiFileUtils or similar class
    /**
     * Gets the path to the resolved {@code ruyi} executable.
     *
     * @return executable path
     */
    public static String getResolvedExecutablePath() {
        final var install = requireInstallPathResult();
        if (install == null || install.isBlank()) {
            throw RuyiCliException.ruyiNotFound();
        }
        return install + File.separator + "ruyi";
    }

    /**
     * Updates package index.
     *
     */
    public static void updatePackageIndex() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).update().end().build();
        request.execute();
    }

    /**
     * Sets repository remote URL.
     *
     * @param remoteUrl remote repository URL
     */
    public static void setRepoRemote(String remoteUrl) {
        RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                .config().set("repo.remote", remoteUrl).end().build().execute();
    }

    /**
     * Gets repository remote URL.
     *
     * @return repository URL or null if not available or command fails
     */
    public static String getRepoRemote() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).config().get("repo.remote").end().build();
        final var result = request.execute();
        return readFirstLine(result);
    }

    /**
     * Sets repository branch.
     *
     * @param branch branch name
     */
    public static void setRepoBranch(String branch) {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).config().set("repo.branch", branch).end().build();
        request.execute();
    }

    /**
     * Gets repository branch.
     *
     * @return branch name or null if not set
     */
    public static String getRepoBranch() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).config().get("repo.branch").end().build();
        final var result = request.execute();
        return readFirstLine(result);
    }

    /**
     * Sets repository local checkout path override.
     *
     * @param localPath local path, or null/empty to unset
     */
    public static void setRepoLocal(String localPath) {
        if (localPath == null || localPath.isBlank()) {
            RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                    .config().unset("repo.local").end().build().execute();
        } else {
            RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                    .config().set("repo.local", localPath).end().build().execute();
        }
    }

    /**
     * Gets repository local path override.
     *
     * @return local path or null if not set
     */
    public static String getRepoLocal() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).config().get("repo.local").end().build();
        final var result = request.execute();
        return readFirstLine(result);
    }

    /**
     * Sets packages.prereleases config.
     *
     * @param enabled true to include pre-release packages
     */
    public static void setPackagesPrereleases(boolean enabled) {
        RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                .config().set("packages.prereleases", enabled ? "true" : "false").end().build()
                .execute();
    }

    /**
     * Gets packages.prereleases config.
     *
     * @return true if pre-releases enabled, false otherwise
     */
    public static boolean getPackagesPrereleases() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).config().get("packages.prereleases").end().build();
        final var result = request.execute();
        return "true".equals(readFirstLine(result));
    }

    /**
     * Sets telemetry mode.
     *
     * @param mode telemetry mode
     */
    public static void setTelemetry(TelemetryMode mode) {
        final var telemetryBuilder = RuyiCliRequest.builder()
                .ruyiInstallDir(requireInstallPathResult()).porcelain(false).telemetry();
        switch (mode) {
            case ON:
                telemetryBuilder.consent();
                break;
            case LOCAL:
                telemetryBuilder.local();
                break;
            case OFF:
                telemetryBuilder.optOut();
                break;
            default:
                telemetryBuilder.consent();
                break;
        }
        final var request = telemetryBuilder.end().build();
        request.execute();
    }

    /**
     * Gets telemetry mode.
     *
     * @return the first line of telemetry status output or null if not available or command fails
     */
    public static String getTelemetryMode() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).telemetry().status().end().build();
        final var result = request.execute();
        return readFirstLine(result);
    }

    private static String readFirstLine(RuyiExecResult result) {
        if (result == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(result.getOutput()))) {
            return reader.readLine();
        } catch (IOException e) {
            // Should not happen with StringReader, but handle it gracefully
            return null;
        }
    }

    /**
     * Uploads telemetry data.
     *
     */
    public static void telemetryUpload() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).telemetry().upload().end().build();
        request.execute();
    }

    /**
     * Lists packages related to an entity.
     *
     * @param entity entity id such as device:milkv-duo
     * @return command result with exit code and captured output
     */
    public static String listRelatedToEntity(String entity) {
        if (entity == null || entity.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid entity");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).experimental(true).list().relatedToEntity(entity).end().build();
        return request.execute().getOutput();
    }

    /**
     * Lists all available packages.
     *
     * @return command result with captured output
     */
    public static String listAllPackages() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).experimental(true).list().nameContains("").end().build();
        return request.execute().getOutput();
    }

    /**
     * Parses package tree output for tree rendering. This is a simple bridge method.
     *
     * @param input raw package tree output
     * @return parsed category and package information for tree construction
     */
    public static List<PackageTreeCategoryInfo> parsePackageTreeFromString(String input) {
        return RuyiCliParsingSupport.parsePackageTreeFromString(input);
    }

    /**
     * Finds installed toolchain package reference for a board.
     *
     * @param boardName board name, with or without {@code device:} prefix
     * @return installed toolchain package reference, or null if none found
     */
    public static String findInstalledToolchainForBoard(String boardName) {
        if (boardName == null || boardName.trim().isEmpty()) {
            return null;
        }

        final var entity = boardName.startsWith("device:") ? boardName : "device:" + boardName;
        final var output = listRelatedToEntity(entity);
        return parseInstalledToolchainFromRelatedEntityOutput(output);
    }

    /**
     * Finds installed toolchain package reference from related-entity output.
     *
     * @param output raw output from {@code ruyi list --related-to-entity}
     * @return installed toolchain package reference, or null if none found
     */
    public static String parseInstalledToolchainFromRelatedEntityOutput(String output) {
        return RuyiCliParsingSupport.findInstalledToolchainFromRelatedEntityOutput(output);
    }

    /**
     * Lists entities by type.
     *
     * @param type entity type such as device
     * @return command result with exit code and captured output
     */
    public static String listEntitiesByType(String type) {
        if (type == null || type.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid entity type");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).experimental(true).entity().list(type).end().build();
        return request.execute().getOutput();
    }

    /**
     * Install a package by name and version (semver). This centralizes construction of the CLI
     * arguments so callers don't build parameter strings themselves.
     *
     * @param name package name
     * @param version package version
     */
    public static void installPackage(String name, String version) {
        if (name == null || name.isBlank() || version == null || version.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid package name or version");
        }
        // Use the documented package atom syntax: name(version)
        // Example: ruyi install 'gnu-upstream(0.20231118.0)'
        final var atom = String.format("%s(%s)", name, version);
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).install().atom(atom).end().build();
        request.execute();
    }

    /**
     * Installs a package with real-time line output and cancellation support.
     *
     * @param packageRef package atom, e.g. name(version)
     * @param lineCallback called for each output line (may be {@code null})
     * @param monitor progress monitor for cancellation (may be {@code null})
     */
    public static void installPackageStreaming(String packageRef, Consumer<String> lineCallback,
            IProgressMonitor monitor) {
        if (packageRef == null || packageRef.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid package reference");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).experimental(true).lineCallback(lineCallback).monitor(monitor)
                .install().atom(packageRef.trim()).end().build();
        request.execute();
    }

    /**
     * Uninstalls a package with real-time line output and cancellation support.
     *
     * @param packageRef package atom, e.g. name(version)
     * @param assumeYes whether to pass -y
     * @param lineCallback called for each output line (may be {@code null})
     * @param monitor progress monitor for cancellation (may be {@code null})
     */
    public static void uninstallPackageStreaming(String packageRef, boolean assumeYes,
            Consumer<String> lineCallback, IProgressMonitor monitor) {
        if (packageRef == null || packageRef.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid package reference");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).experimental(true).lineCallback(lineCallback).monitor(monitor)
                .uninstall().atom(packageRef.trim()).assumeYes(assumeYes).end().build();
        request.execute();
    }

    /**
     * Create a virtual environment. All CLI argument construction stays here so callers pass
     * structured values only.
     *
     * @param path destination filesystem path for the venv (required)
     * @param toolchainName toolchain package name (required)
     * @param toolchainVersion toolchain version (required)
     * @param profile profile name (required)
     * @param emulatorName optional emulator package name (may be null)
     * @param emulatorVersion optional emulator version (may be null)
     */
    public static void createVenv(String path, String toolchainName, String toolchainVersion,
            String profile, String emulatorName, String emulatorVersion) {
        if (path == null || path.isBlank()) {
            throw RuyiCliException.invalidArgument("Empty venv path");
        }
        if (toolchainName == null || toolchainName.isBlank() || toolchainVersion == null
                || toolchainVersion.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid toolchain");
        }
        if (profile == null || profile.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid profile");
        }
        // Pass toolchain/emulator atoms using the documented parentheses syntax
        final var toolchainAtom = String.format("%s(%s)", toolchainName, toolchainVersion);
        String emulatorAtom = null;
        if (emulatorName != null && !emulatorName.isBlank() && emulatorVersion != null
                && !emulatorVersion.isBlank()) {
            emulatorAtom = String.format("%s(%s)", emulatorName, emulatorVersion);
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).venv().profile(profile).dest(path).toolchain(toolchainAtom)
                .emulator(emulatorAtom).end().build();
        request.execute();
    }

    // Package list helpers: call porcelain `list` and parse its JSON-ish output.
    /** Lists available toolchains as reported by the ruyi CLI. */
    public static List<ToolchainInfo> listToolchains() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).list().toolchains().end().build();
        final var result = request.execute();
        return parseToolchainsFromString(result.getOutput());
    }

    /**
     * Parses toolchain list output.
     *
     * @param input raw toolchain list output
     * @return parsed toolchain entries
     */
    public static List<ToolchainInfo> parseToolchainsFromString(String input) {
        return RuyiCliParsingSupport.parseToolchainsFromString(input);
    }

    /** Lists available emulators as reported by the ruyi CLI. */
    public static List<EmulatorInfo> listEmulators() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).list().emulators().end().build();
        final var result = request.execute();
        return parseEmulatorsFromString(result.getOutput());
    }

    /**
     * Parses emulator list output.
     *
     * @param input raw emulator list output
     * @return parsed emulator entries
     */
    public static List<EmulatorInfo> parseEmulatorsFromString(String input) {
        return RuyiCliParsingSupport.parseEmulatorsFromString(input);
    }
}
