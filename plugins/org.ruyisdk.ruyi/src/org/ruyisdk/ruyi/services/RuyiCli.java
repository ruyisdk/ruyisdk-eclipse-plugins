package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
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
        final var out = new ArrayList<ProfileInfo>();
        try {
            var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                    .porcelain(true).list().profiles().end().build();
            var result = request.execute();
            var output = result.getOutput();

            if (output == null || output.isEmpty()) {
                return out;
            }

            // plain text parsing: lines like "wch-qingke-v2a (needs quirks: {'wch'})"
            // which quirks are in the form of a Python set literal.
            final var namePtn = Pattern.compile("^\\s*([^\\s(]+)", Pattern.CASE_INSENSITIVE);
            final var quirksPtn =
                    Pattern.compile("needs quirks:\\s*\\{([^}]*)\\}", Pattern.CASE_INSENSITIVE);
            final var reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(output.getBytes())));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                final var m = namePtn.matcher(line);
                String name = null;
                if (m.find()) {
                    name = m.group(1);
                }
                final var quirks = new ArrayList<String>();
                final var q = quirksPtn.matcher(line);
                if (q.find()) {
                    var raw = q.group(1).trim();
                    // normalize quotes and whitespace
                    raw = raw.replaceAll("'", "").replaceAll("\"", "");
                    for (final var s : raw.split(",")) {
                        final var t = s.trim();
                        if (!t.isEmpty()) {
                            quirks.add(t);
                        }
                    }
                }
                if (name != null && !name.isEmpty()) {
                    out.add(new ProfileInfo(name, quirks));
                }
            }

        } catch (IOException e) {
            throw RuyiCliException.ioError(e);
        }
        return out;
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
        // news
        // content.
        // The hang is due to a unresolved bug.
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).timeoutSeconds(3).news().read(idOrOrdinal).end().build();
        final var result = request.execute();
        return parseNewsReadFromString(result.getOutput());
    }

    /** Parses the output of a news list command. */
    public static List<NewsListItemInfo> parseNewsListFromString(String input) {
        final var out = new ArrayList<NewsListItemInfo>();
        if (input == null || input.isBlank()) {
            return out;
        }
        for (final var o : parseConcatenatedJsonObjects(input)) {
            if (o == null) {
                continue;
            }
            final var ty = o.optString("ty", null);
            if (!"newsitem-v1".equalsIgnoreCase(ty)) {
                continue;
            }
            final var id = o.optString("id", null);
            final var ord = o.optIntegerObject("ord", null);
            final var isRead = o.optBooleanObject("is_read", null);
            final var langs = o.optJSONArray("langs");
            final var title = chooseNewsDisplayTitle(langs);
            out.add(new NewsListItemInfo(id, ord, isRead, title));
        }
        return out;
    }

    /** Parses the output of a news read command. */
    public static NewsReadResult parseNewsReadFromString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        final var objs = parseConcatenatedJsonObjects(input);
        if (objs.isEmpty()) {
            return null;
        }
        final var o = objs.get(0);
        if (o == null) {
            return null;
        }
        final var id = o.optString("id", null);
        final var ord = o.optIntegerObject("ord", null);
        final var isRead = o.optBooleanObject("is_read", null);
        final var langs = o.optJSONArray("langs");
        final var title = chooseNewsDisplayTitle(langs);
        final var content = chooseNewsContent(langs);
        return new NewsReadResult(id, ord, isRead, title, content);
    }

    private static List<JSONObject> parseConcatenatedJsonObjects(String input) {
        final var out = new ArrayList<JSONObject>();
        if (input == null) {
            return out;
        }
        final var t = new JSONTokener(input);
        while (true) {
            final var c = t.nextClean();
            if (c == 0) {
                break;
            }
            t.back();
            final var v = t.nextValue();
            if (v instanceof JSONObject) {
                out.add((JSONObject) v);
            } else if (v instanceof JSONArray) {
                final var arr = (JSONArray) v;
                for (int i = 0; i < arr.length(); i++) {
                    final var el = arr.get(i);
                    if (el instanceof JSONObject) {
                        out.add((JSONObject) el);
                    }
                }
            } else {
                // ignore other values
            }
        }
        return out;
    }

    private static String chooseNewsDisplayTitle(JSONArray langs) {
        final var best = chooseBestNewsLang(langs);
        if (best == null) {
            return null;
        }
        return best.optString("display_title", null);
    }

    private static String chooseNewsContent(JSONArray langs) {
        final var best = chooseBestNewsLang(langs);
        if (best == null) {
            return null;
        }
        return best.optString("content", null);
    }

    private static JSONObject chooseBestNewsLang(JSONArray langs) {
        if (langs == null || langs.length() == 0) {
            return null;
        }

        // Prefer an explicit English entry if available, else match current locale, else first.
        JSONObject first = null;
        final var sysLang = Locale.getDefault() == null ? "" : Locale.getDefault().toString();
        for (int i = 0; i < langs.length(); i++) {
            final var o = langs.optJSONObject(i);
            if (o == null) {
                continue;
            }
            if (first == null) {
                first = o;
            }
            final var lang = o.optString("lang", "");
            if ("en_US".equalsIgnoreCase(lang)) {
                return o;
            }
        }
        if (!sysLang.isEmpty()) {
            for (int i = 0; i < langs.length(); i++) {
                final var o = langs.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                final var lang = o.optString("lang", "");
                if (sysLang.equalsIgnoreCase(lang)) {
                    return o;
                }
            }
        }
        return first;
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
     * Gets installed Ruyi version.
     *
     * @return version or null if not available or command fails
     */
    public static RuyiVersion getInstalledVersion() {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(false).args("-V").build();
        final var result = request.execute();
        return parseInstalledVersion(result);
    }

    private static RuyiVersion parseInstalledVersion(RuyiExecResult result) {
        if (result == null) {
            return null;
        }

        final var pattern = Pattern.compile("^Ruyi\\s+(\\d+\\.\\d+\\.\\d+)\\b");
        final var matcher = pattern.matcher(result.getOutput());

        if (!matcher.find()) {
            return null;
        }
        final var versionStr = matcher.group(1);
        return RuyiVersion.parse(versionStr);
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
        final var atom = name + "(" + version + ")";
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
        final var toolchainAtom = toolchainName + "(" + toolchainVersion + ")";
        String emulatorAtom = null;
        if (emulatorName != null && !emulatorName.isBlank() && emulatorVersion != null
                && !emulatorVersion.isBlank()) {
            emulatorAtom = emulatorName + "(" + emulatorVersion + ")";
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).venv().profile(profile).dest(path).toolchain(toolchainAtom)
                .emulator(emulatorAtom).end().build();
        request.execute();
    }

    // TODO: move to data parsing class
    // Extract top-level JSON objects from a concatenated stream (handles multiple back-to-back
    // objects).
    private static List<String> extractJsonObjects(String s) {
        final var objs = new ArrayList<String>();
        if (s == null || s.isEmpty()) {
            return objs;
        }
        int depth = 0;
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objs.add(s.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objs;
    }

    // Package list helpers: call porcelain `list` and parse its JSON-ish output.
    /** Lists available toolchains as reported by the ruyi CLI. */
    public static List<ToolchainInfo> listToolchains() {
        final var out = new ArrayList<ToolchainInfo>();
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).list().toolchains().end().build();
        final var result = request.execute();
        final var output = result.getOutput();
        if (output == null || output.isEmpty()) {
            return out;
        }
        out.addAll(parseToolchainsFromString(output));
        return out;
    }

    // TODO: move to data parsing class
    /**
     * Parse toolchain package objects from a porcelain output string (may contain concatenated JSON
     * objects). This helper is public to make parsing testable.
     */
    public static List<ToolchainInfo> parseToolchainsFromString(String input) {
        final var out = new ArrayList<ToolchainInfo>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        final var objs = extractJsonObjects(input);
        for (final var jo : objs) {
            if (jo == null || jo.isBlank()) {
                continue;
            }
            final var o = new JSONObject(jo);
            final var category = o.optString("category", "");
            if (!"toolchain".equalsIgnoreCase(category)) {
                continue;
            }
            final var pkgName = o.optString("name", "").trim();
            if (pkgName.isEmpty()) {
                continue;
            }
            final var versions = new ArrayList<String>();
            final var vers = o.optJSONArray("vers");
            if (vers != null && vers.length() > 0) {
                for (int vi = 0; vi < vers.length(); vi++) {
                    final var v = vers.optJSONObject(vi);
                    if (v == null) {
                        continue;
                    }
                    final var sem = v.optString("semver", v.optString("version", "")).trim();
                    if (sem != null && !sem.isEmpty()) {
                        versions.add(sem);
                    }
                }
            }
            // Extract quirks (flavors) from the first version's metadata
            final var quirks = extractPackageQuirks(vers, "toolchain");
            // only expose packages for which we actually know at least one version
            if (!versions.isEmpty()) {
                out.add(new ToolchainInfo(pkgName, versions, quirks));
            }
        }
        return out;
    }

    /** Lists available emulators as reported by the ruyi CLI. */
    public static List<EmulatorInfo> listEmulators() {
        final var out = new ArrayList<EmulatorInfo>();
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                .porcelain(true).list().emulators().end().build();
        final var result = request.execute();
        final var output = result.getOutput();
        if (output == null || output.isEmpty()) {
            return out;
        }
        out.addAll(parseEmulatorsFromString(output));
        return out;
    }

    // TODO: move to data parsing class
    /**
     * Parse emulator package objects from a porcelain output string (may contain concatenated JSON
     * objects). This helper is public to make parsing testable.
     */
    public static List<EmulatorInfo> parseEmulatorsFromString(String input) {
        final var out = new ArrayList<EmulatorInfo>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        final var objs = extractJsonObjects(input);
        for (final var jo : objs) {
            if (jo == null || jo.isBlank()) {
                continue;
            }
            final var o = new JSONObject(jo);
            final var category = o.optString("category", "");
            if (!"emulator".equalsIgnoreCase(category)) {
                continue;
            }
            final var pkgName = o.optString("name", "").trim();
            if (pkgName.isEmpty()) {
                continue;
            }
            final var versions = new ArrayList<String>();
            final var vers = o.optJSONArray("vers");
            if (vers != null && vers.length() > 0) {
                for (int vi = 0; vi < vers.length(); vi++) {
                    final var v = vers.optJSONObject(vi);
                    if (v == null) {
                        continue;
                    }
                    final var sem = v.optString("semver", v.optString("version", "")).trim();
                    if (sem != null && !sem.isEmpty()) {
                        versions.add(sem);
                    }
                }
            }
            // Extract quirks (flavors) from the first version's metadata
            final var quirks = extractPackageQuirks(vers, "emulator");
            if (!versions.isEmpty()) {
                out.add(new EmulatorInfo(pkgName, versions, quirks));
            }
        }
        return out;
    }

    /**
     * Extracts quirks (flavors) from the first version entry that contains the given metadata key
     * (e.g. "toolchain" or "emulator") inside the {@code pm} object.
     */
    private static List<String> extractPackageQuirks(JSONArray vers, String metadataKey) {
        if (vers == null || vers.length() == 0) {
            return List.of();
        }
        final var quirksSet = new LinkedHashSet<String>();
        for (int vi = 0; vi < vers.length(); vi++) {
            final var v = vers.optJSONObject(vi);
            if (v == null) {
                continue;
            }
            final var pm = v.optJSONObject("pm");
            if (pm == null) {
                continue;
            }
            final var meta = pm.optJSONObject(metadataKey);
            if (meta == null) {
                continue;
            }
            collectJsonArrayStrings(meta.optJSONArray("quirks"), quirksSet);
            collectJsonArrayStrings(meta.optJSONArray("flavors"), quirksSet);
            break; // all versions should have the same quirks, so use first version's metadata
        }
        return new ArrayList<>(quirksSet);
    }

    private static void collectJsonArrayStrings(JSONArray arr, Collection<String> target) {
        if (arr == null) {
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            final var s = arr.optString(i, "").trim();
            if (!s.isEmpty()) {
                target.add(s);
            }
        }
    }
}
