package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.ruyi.services.RuyiProperties.TelemetryStatus;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Simple DTOs returned by the CLI wrapper to avoid cross-plugin model coupling.
 */
public class RuyiCli {
    /** Profile information returned by the ruyi CLI. */
    public static class ProfileInfo {
        private final String name;
        private final String quirks;

        /**
         * Creates an instance.
         *
         * @param name profile name
         * @param quirks profile quirks description
         */
        public ProfileInfo(String name, String quirks) {
            this.name = name;
            this.quirks = quirks;
        }

        public String getName() {
            return name;
        }

        public String getQuirks() {
            return quirks;
        }
    }

    /** Toolchain package information returned by the ruyi CLI. */
    public static class ToolchainInfo {
        private final String name;
        private final List<String> versions;

        /**
         * Creates an instance.
         *
         * @param name toolchain package name
         * @param versions available toolchain versions
         */
        public ToolchainInfo(String name, List<String> versions) {
            this.name = name;
            this.versions = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
        }

        public String getName() {
            return name;
        }

        public List<String> getVersions() {
            return Collections.unmodifiableList(versions);
        }
    }

    /** Emulator package information returned by the ruyi CLI. */
    public static class EmulatorInfo {
        private final String name;
        private final List<String> versions;

        /**
         * Creates an instance.
         *
         * @param name emulator package name
         * @param versions available emulator versions
         */
        public EmulatorInfo(String name, List<String> versions) {
            this.name = name;
            this.versions = versions == null ? new ArrayList<>() : new ArrayList<>(versions);
        }

        public String getName() {
            return name;
        }

        public List<String> getVersions() {
            return Collections.unmodifiableList(versions);
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
            var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true).list()
                            .profiles().end().build();
            var result = request.execute();
            var output = result.getOutput();

            if (output == null || output.isEmpty()) {
                return out;
            }

            final var trimmed = output.trim();
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                // try parse JSON array/object
                try {
                    final var arr = new JSONArray(trimmed);
                    for (int i = 0; i < arr.length(); i++) {
                        final var o = arr.getJSONObject(i);
                        final var name = o.optString("name", o.optString("id", ""));
                        final var quirks = o.optString("quirks", "");
                        out.add(new ProfileInfo(name, quirks));
                    }
                    return out;
                } catch (Exception e) {
                    // fallthrough to plain parsing
                }
            }

            // plain text parsing: lines like "wch-qingke-v2a (needs quirks: {'wch'})"
            final var namePtn = Pattern.compile("^\\s*([^\\s(]+)", Pattern.CASE_INSENSITIVE);
            final var quirksPtn = Pattern.compile("needs quirks:\\s*\\{([^}]*)\\}", Pattern.CASE_INSENSITIVE);
            final var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.getBytes())));
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
                String quirks = "";
                final var q = quirksPtn.matcher(line);
                if (q.find()) {
                    quirks = q.group(1).trim();
                    // normalize quotes and whitespace
                    quirks = quirks.replaceAll("\\\'", "").replaceAll("\\\"", "");
                }
                if (name != null && !name.isEmpty()) {
                    out.add(new ProfileInfo(name, quirks));
                }
            }

        } catch (Exception e) {
            // ignore - return empty list
        }
        return out;
    }

    /** Lists available news items using the ruyi CLI. */
    public static List<NewsListItemInfo> listNewsItems(boolean onlyUnread) {
        try {
            final var result = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                            .news().list(onlyUnread).execute();
            return parseNewsListFromString(result.getOutput());
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    /** Reads a news item by ID or ordinal using the ruyi CLI. */
    public static NewsReadResult readNewsItem(String idOrOrdinal) {
        try {
            if (idOrOrdinal == null || idOrOrdinal.isBlank()) {
                return null;
            }
            // TODO: the 3-second timeout help us avoid hanging if the CLI gets stuck trying to read news
            // content.
            // The hang is due to a unresolved bug.
            final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                            .timeoutSeconds(3).news().read(idOrOrdinal).end().build();
            final var result = request.execute();
            return parseNewsReadFromString(result.getOutput());
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses the output of a news list command. */
    public static List<NewsListItemInfo> parseNewsListFromString(String input) {
        final var out = new ArrayList<NewsListItemInfo>();
        if (input == null || input.isBlank()) {
            return out;
        }
        try {
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
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /** Parses the output of a news read command. */
    public static NewsReadResult parseNewsReadFromString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
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
        } catch (Exception e) {
            return null;
        }
    }

    private static List<JSONObject> parseConcatenatedJsonObjects(String input) {
        final var out = new ArrayList<JSONObject>();
        if (input == null) {
            return out;
        }
        try {
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
        } catch (Exception e) {
            // ignore
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
        try {
            return RuyiFileUtils.findInstallPathWithRuyi();
        } catch (Exception e) {
            return "";
        }
    }

    private static String requireInstallPath() throws IOException {
        final var install = requireInstallPathResult();
        if (install == null || install.isBlank()) {
            throw new IOException("ruyi executable not found in configured or default install path");
        }
        return install;
    }

    /**
     * Runs a command with experimental environment and optional working directory.
     *
     * @param args arguments without the `ruyi` executable
     * @param workingDirectory working directory for the process (may be {@code null})
     * @return command result with exit code and captured output
     * @throws RuyiCliException if the command fails
     */
    public static String runRuyiExperimental(List<String> args, File workingDirectory) throws RuyiCliException {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                        .args(args).experimental(true).workingDirectory(workingDirectory).build();
        return request.execute().getOutput();
    }

    // TODO: move to RuyiFileUtils or similar class
    /**
     * Gets the path to the resolved {@code ruyi} executable.
     *
     * @return executable path
     *
     * @throws IOException if no executable can be resolved
     */
    public static String getResolvedExecutablePath() throws IOException {
        return requireInstallPath() + File.separator + "ruyi";
    }

    /**
     * Updates package index.
     *
     * @throws RuyiCliException if update fails
     */
    public static void updatePackageIndex() throws RuyiCliException {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true).update()
                        .end().build();
        request.execute();
    }

    /**
     * Gets installed Ruyi version.
     *
     * @return version or null if not available or command fails
     */
    public static RuyiVersion getInstalledVersion() {
        try {
            final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                            .args("-V").build();
            final var result = request.execute();
            return parseInstalledVersion(result);
        } catch (RuyiCliException e) {
            return null;
        }
    }

    private static RuyiVersion parseInstalledVersion(RuyiExecResult result) {
        if (result == null) {
            return null;
        }

        try (final var reader = new BufferedReader(new StringReader(result.getOutput()))) {
            final var firstLine = reader.readLine();
            if (firstLine == null) {
                return null;
            }

            final var prefix = "Ruyi ";
            if (firstLine.startsWith(prefix)) {
                final var versionStr = firstLine.substring(prefix.length()).trim();
                if (versionStr.matches("^\\d+\\.\\d+\\.\\d+$")) {
                    return RuyiVersion.parse(versionStr);
                }
            }
        } catch (IOException e) {
            // Should not happen with StringReader, but handle it gracefully
            return null;
        }
        return null;
    }

    /**
     * Sets repository remote URL.
     *
     * @param repoUrl repository URL
     * @throws RuyiCliException if command fails
     */
    public static void setRepoRemote(String repoUrl) throws RuyiCliException {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                        .config().set("repo.remote", repoUrl).end().build();
        request.execute();
    }

    /**
     * Gets repository remote URL.
     *
     * @return repository URL or null if not available or command fails
     */
    public static String getRepoRemote() {
        try {
            final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                            .config().get("repo.remote").end().build();
            final var result = request.execute();
            return readFirstLine(result);
        } catch (RuyiCliException e) {
            return null;
        }
    }

    /**
     * Sets telemetry mode.
     *
     * @param status telemetry status
     * @throws RuyiCliException if command fails
     */
    public static void setTelemetry(TelemetryStatus status) throws RuyiCliException {
        final var telemetryBuilder = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult())
                        .porcelain(false).telemetry();
        switch (status) {
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
     * Gets telemetry status.
     *
     * @return telemetry status output line or null if not available or command fails
     */
    public static String getTelemetryStatus() {
        try {
            final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                            .telemetry().status().end().build();
            final var result = request.execute();
            return readFirstLine(result);
        } catch (RuyiCliException e) {
            return null;
        }
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
     * @throws RuyiCliException if upload fails
     */
    public static void telemetryUpload() throws RuyiCliException {
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                        .telemetry().upload().end().build();
        request.execute();
    }

    /**
     * Lists packages related to an entity.
     *
     * @param entity entity id such as device:milkv-duo
     * @return command result with exit code and captured output
     * @throws RuyiCliException if the command fails
     */
    public static String listRelatedToEntity(String entity) throws RuyiCliException {
        if (entity == null || entity.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid entity");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                        .experimental(true).list().relatedToEntity(entity).end().build();
        return request.execute().getOutput();
    }

    /**
     * Lists entities by type.
     *
     * @param type entity type such as device
     * @return command result with exit code and captured output
     * @throws RuyiCliException if the command fails
     */
    public static String listEntitiesByType(String type) throws RuyiCliException {
        if (type == null || type.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid entity type");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                        .experimental(true).entity().list(type).end().build();
        return request.execute().getOutput();
    }

    /**
     * Install a package by name and version (semver). This centralizes construction of the CLI
     * arguments so callers don't build parameter strings themselves.
     *
     * @param name package name
     * @param version package version
     * @throws RuyiCliException if the command fails
     */
    public static void installPackage(String name, String version) throws RuyiCliException {
        if (name == null || name.isBlank() || version == null || version.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid package name or version");
        }
        // Use the documented package atom syntax: name(version)
        // Example: ruyi install 'gnu-upstream(0.20231118.0)'
        final var atom = name + "(" + version + ")";
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                        .install().atom(atom).end().build();
        request.execute();
    }

    /**
     * Installs a package with real-time line output and cancellation support.
     *
     * @param packageRef package atom, e.g. name(version)
     * @param lineCallback called for each output line (may be {@code null})
     * @param monitor progress monitor for cancellation (may be {@code null})
     * @throws RuyiCliException if the command fails
     */
    public static void installPackageStreaming(String packageRef, Consumer<String> lineCallback,
                    IProgressMonitor monitor) throws RuyiCliException {
        if (packageRef == null || packageRef.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid package reference");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                        .experimental(true).lineCallback(lineCallback).monitor(monitor).install()
                        .atom(packageRef.trim()).end().build();
        request.execute();
    }

    /**
     * Uninstalls a package with real-time line output and cancellation support.
     *
     * @param packageRef package atom, e.g. name(version)
     * @param assumeYes whether to pass -y
     * @param lineCallback called for each output line (may be {@code null})
     * @param monitor progress monitor for cancellation (may be {@code null})
     * @throws RuyiCliException if the command fails
     */
    public static void uninstallPackageStreaming(String packageRef, boolean assumeYes, Consumer<String> lineCallback,
                    IProgressMonitor monitor) throws RuyiCliException {
        if (packageRef == null || packageRef.isBlank()) {
            throw RuyiCliException.invalidArgument("Invalid package reference");
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(false)
                        .experimental(true).lineCallback(lineCallback).monitor(monitor).uninstall()
                        .atom(packageRef.trim()).assumeYes(assumeYes).end().build();
        request.execute();
    }

    /**
     * Create a virtual environment. All CLI argument construction stays here so callers pass structured
     * values only.
     *
     * @param path destination filesystem path for the venv (required)
     * @param toolchainName toolchain package name (required)
     * @param toolchainVersion toolchain version (required)
     * @param profile profile name (required)
     * @param emulatorName optional emulator package name (may be null)
     * @param emulatorVersion optional emulator version (may be null)
     * @throws RuyiCliException if the command fails or arguments are invalid
     */
    public static void createVenv(String path, String toolchainName, String toolchainVersion, String profile,
                    String emulatorName, String emulatorVersion) throws RuyiCliException {
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
        if (emulatorName != null && !emulatorName.isBlank() && emulatorVersion != null && !emulatorVersion.isBlank()) {
            emulatorAtom = emulatorName + "(" + emulatorVersion + ")";
        }
        final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true).venv()
                        .profile(profile).dest(path).toolchain(toolchainAtom).emulator(emulatorAtom).end().build();
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
        try {
            final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                            .list().toolchains().end().build();
            final var result = request.execute();
            final var output = result.getOutput();
            if (output == null || output.isEmpty()) {
                return out;
            }
            out.addAll(parseToolchainsFromString(output));
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    // TODO: move to data parsing class
    /**
     * Parse toolchain package objects from a porcelain output string (may contain concatenated JSON
     * objects). This helper is public to make parsing testable.
     */
    public static List<ToolchainInfo> parseToolchainsFromString(String input) {
        final var out = new ArrayList<ToolchainInfo>();
        try {
            if (input == null || input.isEmpty()) {
                return out;
            }
            final var objs = extractJsonObjects(input);
            for (final var jo : objs) {
                if (jo == null || jo.isBlank()) {
                    continue;
                }
                try {
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
                            try {
                                final var v = vers.optJSONObject(vi);
                                if (v == null) {
                                    continue;
                                }
                                final var sem = v.optString("semver", v.optString("version", "")).trim();
                                if (sem != null && !sem.isEmpty()) {
                                    versions.add(sem);
                                }
                            } catch (Exception ex) {
                                // ignore individual version parse problems
                            }
                        }
                    }
                    // only expose packages for which we actually know at least one version
                    if (!versions.isEmpty()) {
                        out.add(new ToolchainInfo(pkgName, versions));
                    }
                } catch (Exception e) {
                    // ignore malformed object and continue parsing others
                }
            }
        } catch (Exception outer) {
            // defensive: never fail the caller due to parsing problems
        }
        return out;
    }

    /** Lists available emulators as reported by the ruyi CLI. */
    public static List<EmulatorInfo> listEmulators() {
        final var out = new ArrayList<EmulatorInfo>();
        try {
            final var request = RuyiCliRequest.builder().ruyiInstallDir(requireInstallPathResult()).porcelain(true)
                            .list().emulators().end().build();
            final var result = request.execute();
            final var output = result.getOutput();
            if (output == null || output.isEmpty()) {
                return out;
            }
            out.addAll(parseEmulatorsFromString(output));
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    // TODO: move to data parsing class
    /**
     * Parse emulator package objects from a porcelain output string (may contain concatenated JSON
     * objects). This helper is public to make parsing testable.
     */
    public static List<EmulatorInfo> parseEmulatorsFromString(String input) {
        final var out = new ArrayList<EmulatorInfo>();
        try {
            if (input == null || input.isEmpty()) {
                return out;
            }
            final var objs = extractJsonObjects(input);
            for (final var jo : objs) {
                if (jo == null || jo.isBlank()) {
                    continue;
                }
                try {
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
                            try {
                                final var v = vers.optJSONObject(vi);
                                if (v == null) {
                                    continue;
                                }
                                final var sem = v.optString("semver", v.optString("version", "")).trim();
                                if (sem != null && !sem.isEmpty()) {
                                    versions.add(sem);
                                }
                            } catch (Exception ex) {
                                // ignore individual version parse problems
                            }
                        }
                    }
                    if (!versions.isEmpty()) {
                        out.add(new EmulatorInfo(pkgName, versions));
                    }
                } catch (Exception e) {
                    // ignore malformed object and continue parsing others
                }
            }
        } catch (Exception outer) {
            // defensive: never fail the caller due to parsing problems
        }
        return out;
    }

    static class RuyiCliRequest {

        private final String ruyiInstallDir;
        private final boolean porcelain;
        private final String command;
        private final List<String> args;
        private final Map<String, String> environment;
        private final File workingDirectory;
        private final Consumer<String> lineCallback;
        private final IProgressMonitor monitor;
        private final int timeoutSeconds;

        private RuyiCliRequest(Builder builder) {
            this.ruyiInstallDir = builder.ruyiInstallDir;
            this.porcelain = builder.porcelain;
            this.command = builder.command;
            this.args = builder.args == null ? Collections.emptyList() : new ArrayList<>(builder.args);
            this.environment = builder.environment == null ? null : new HashMap<>(builder.environment);
            this.workingDirectory = builder.workingDirectory;
            this.lineCallback = builder.lineCallback;
            this.monitor = builder.monitor;
            this.timeoutSeconds = builder.timeoutSeconds;
        }

        public static Builder builder() {
            return new Builder();
        }

        public RuyiExecResult execute() throws RuyiCliException {
            if (ruyiInstallDir == null || ruyiInstallDir.isBlank()) {
                throw RuyiCliException.ruyiNotFound();
            }

            final var cmdArgs = buildCommandArgs();
            final var cmdString = buildCommandString(cmdArgs);
            try {
                final var result = RuyiCliExecutor.execute(ruyiInstallDir, environment, workingDirectory, lineCallback,
                                monitor, timeoutSeconds, cmdArgs.toArray(new String[0]));
                if (result.getExitCode() != 0) {
                    // Check if the output indicates a timeout
                    if (result.getOutput() != null && result.getOutput().contains("ruyi command timed out")) {
                        throw RuyiCliException.timeout(timeoutSeconds);
                    }
                    throw RuyiCliException.executionFailed(result.getOutput(), cmdString);
                }
                return result;
            } catch (IOException e) {
                throw RuyiCliException.ioError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw RuyiCliException.cancelled();
            } catch (OperationCanceledException e) {
                throw RuyiCliException.cancelled();
            }
        }

        private List<String> buildCommandArgs() {
            final var cmdArgs = new ArrayList<String>();
            if (porcelain) {
                cmdArgs.add("--porcelain");
            }
            if (command != null && !command.isBlank()) {
                cmdArgs.add(command);
            }
            cmdArgs.addAll(args);
            return cmdArgs;
        }

        private String buildCommandString(List<String> cmdArgs) {
            final var command = new ArrayList<String>();
            command.add(ruyiInstallDir + File.separator + "ruyi");
            command.addAll(cmdArgs);
            final var builder = new StringBuilder();
            for (int i = 0; i < command.size(); i++) {
                final var part = command.get(i);
                if (part.contains(" ")) {
                    builder.append('\'').append(part).append('\'');
                } else {
                    builder.append(part);
                }
                if (i < command.size() - 1) {
                    builder.append(' ');
                }
            }
            return builder.toString();
        }

        /**
         * Builder for {@link RuyiCliRequest}.
         */
        public static final class Builder {

            private String ruyiInstallDir;
            private boolean porcelain;
            private String command;
            private List<String> args;
            private Map<String, String> environment;
            private File workingDirectory;
            private Consumer<String> lineCallback;
            private IProgressMonitor monitor;
            private int timeoutSeconds;

            private Builder() {}

            public Builder ruyiInstallDir(String value) {
                this.ruyiInstallDir = value;
                return this;
            }

            public Builder porcelain(boolean value) {
                this.porcelain = value;
                return this;
            }

            public Builder command(String value) {
                this.command = value;
                return this;
            }

            public Builder args(List<String> value) {
                if (value == null) {
                    return this;
                }
                if (this.args == null) {
                    this.args = new ArrayList<>();
                }
                this.args.addAll(value);
                return this;
            }

            public Builder args(String... value) {
                return args(value == null ? null : Arrays.asList(value));
            }

            public Builder environment(Map<String, String> value) {
                this.environment = value == null ? null : new HashMap<>(value);
                return this;
            }

            public Builder workingDirectory(File value) {
                this.workingDirectory = value;
                return this;
            }

            public Builder lineCallback(Consumer<String> value) {
                this.lineCallback = value;
                return this;
            }

            public Builder monitor(IProgressMonitor value) {
                this.monitor = value;
                return this;
            }

            public Builder timeoutSeconds(int value) {
                this.timeoutSeconds = value;
                return this;
            }

            /**
             * Enables or disables experimental mode by setting the RUYI_EXPERIMENTAL environment variable.
             *
             * @param value if true, enable experimental mode
             * @return this builder
             */
            public Builder experimental(boolean value) {
                if (value) {
                    if (this.environment == null) {
                        this.environment = new HashMap<>();
                    }
                    this.environment.put("RUYI_EXPERIMENTAL", "true");
                } else if (this.environment != null) {
                    this.environment.remove("RUYI_EXPERIMENTAL");
                }
                return this;
            }

            /**
             * Creates a news command builder for list/read operations.
             *
             * @return news command builder
             */
            public NewsCommandBuilder news() {
                return new NewsCommandBuilder(this);
            }

            /**
             * Creates a list command builder for listing packages/profiles.
             *
             * @return list command builder
             */
            public ListCommandBuilder list() {
                return new ListCommandBuilder(this);
            }

            /**
             * Creates an install command builder.
             *
             * @return install command builder
             */
            public InstallCommandBuilder install() {
                return new InstallCommandBuilder(this);
            }

            /**
             * Creates an uninstall command builder.
             *
             * @return uninstall command builder
             */
            public UninstallCommandBuilder uninstall() {
                return new UninstallCommandBuilder(this);
            }

            /**
             * Creates a venv command builder.
             *
             * @return venv command builder
             */
            public VenvCommandBuilder venv() {
                return new VenvCommandBuilder(this);
            }

            /**
             * Creates a config command builder.
             *
             * @return config command builder
             */
            public ConfigCommandBuilder config() {
                return new ConfigCommandBuilder(this);
            }

            /**
             * Creates a telemetry command builder.
             *
             * @return telemetry command builder
             */
            public TelemetryCommandBuilder telemetry() {
                return new TelemetryCommandBuilder(this);
            }

            /**
             * Creates an update command builder.
             *
             * @return update command builder
             */
            public UpdateCommandBuilder update() {
                return new UpdateCommandBuilder(this);
            }

            /**
             * Creates an entity command builder.
             *
             * @return entity command builder
             */
            public EntityCommandBuilder entity() {
                return new EntityCommandBuilder(this);
            }

            public RuyiCliRequest build() {
                return new RuyiCliRequest(this);
            }
        }

        /**
         * Builder for news commands (list, read). Only contains Ruyi-specific options. Non-Ruyi settings
         * (environment, lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class NewsCommandBuilder {
            private final Builder parent;
            private String subcommand;
            private final List<String> subArgs = new ArrayList<>();

            private NewsCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("news");
            }

            /**
             * Lists news items.
             *
             * @param onlyUnread if true, list only unread items
             * @return this builder
             */
            public NewsCommandBuilder list(boolean onlyUnread) {
                this.subcommand = "list";
                if (onlyUnread) {
                    subArgs.add("--new");
                }
                return this;
            }

            /**
             * Lists all news items.
             *
             * @return this builder
             */
            public NewsCommandBuilder list() {
                return list(false);
            }

            /**
             * Lists only unread news items.
             *
             * @return this builder
             */
            public NewsCommandBuilder listUnread() {
                return list(true);
            }

            /**
             * Reads a news item by ID or ordinal.
             *
             * @param idOrOrdinal news item ID or ordinal
             * @return this builder
             */
            public NewsCommandBuilder read(String idOrOrdinal) {
                this.subcommand = "read";
                subArgs.add("--quiet");
                subArgs.add(idOrOrdinal);
                return this;
            }

            /**
             * Finishes news command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (subcommand != null) {
                    parent.args(subcommand);
                    parent.args(subArgs);
                }
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for list commands (profiles, toolchains, emulators). Only contains Ruyi-specific options.
         * Non-Ruyi settings (environment, lineCallback, etc.) must be configured on the parent Builder
         * before calling this.
         */
        public static class ListCommandBuilder {
            private final Builder parent;
            private String subcommand;
            private final List<String> subArgs = new ArrayList<>();

            private ListCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("list");
            }

            /**
             * Lists available profiles.
             *
             * @return this builder
             */
            public ListCommandBuilder profiles() {
                this.subcommand = "profiles";
                return this;
            }

            /**
             * Lists toolchains.
             *
             * @return this builder
             */
            public ListCommandBuilder toolchains() {
                subArgs.add("--category-is");
                subArgs.add("toolchain");
                subArgs.add("--name-contains");
                subArgs.add("");
                return this;
            }

            /**
             * Lists emulators.
             *
             * @return this builder
             */
            public ListCommandBuilder emulators() {
                subArgs.add("--category-is");
                subArgs.add("emulator");
                subArgs.add("--name-contains");
                subArgs.add("");
                return this;
            }

            /**
             * Lists packages related to an entity.
             *
             * @param entity entity ID
             * @return this builder
             */
            public ListCommandBuilder relatedToEntity(String entity) {
                subArgs.add("--related-to-entity");
                subArgs.add(entity);
                return this;
            }

            /**
             * Finishes list command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (subcommand != null) {
                    parent.args(subcommand);
                }
                parent.args(subArgs);
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for install commands. Only contains Ruyi-specific options. Non-Ruyi settings
         * (environment, lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class InstallCommandBuilder {
            private final Builder parent;
            private final List<String> atoms = new ArrayList<>();
            private boolean fetchOnly;
            private String host;
            private boolean reinstall;

            private InstallCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("install");
            }

            /**
             * Adds a package atom to install.
             *
             * @param atom package atom (e.g., "name(version)")
             * @return this builder
             */
            public InstallCommandBuilder atom(String atom) {
                atoms.add(atom);
                return this;
            }

            /**
             * Sets fetch-only mode.
             *
             * @param fetchOnly if true, only fetch without installing
             * @return this builder
             */
            public InstallCommandBuilder fetchOnly(boolean fetchOnly) {
                this.fetchOnly = fetchOnly;
                return this;
            }

            /**
             * Sets host architecture override.
             *
             * @param host host architecture
             * @return this builder
             */
            public InstallCommandBuilder host(String host) {
                this.host = host;
                return this;
            }

            /**
             * Sets reinstall flag.
             *
             * @param reinstall if true, force re-installation
             * @return this builder
             */
            public InstallCommandBuilder reinstall(boolean reinstall) {
                this.reinstall = reinstall;
                return this;
            }

            /**
             * Finishes install command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (fetchOnly) {
                    parent.args("-f");
                }
                if (host != null && !host.isBlank()) {
                    parent.args("--host", host);
                }
                if (reinstall) {
                    parent.args("--reinstall");
                }
                parent.args(atoms);
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for uninstall commands. Only contains Ruyi-specific options. Non-Ruyi settings
         * (environment, lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class UninstallCommandBuilder {
            private final Builder parent;
            private final List<String> atoms = new ArrayList<>();
            private boolean assumeYes;

            private UninstallCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("uninstall");
            }

            /**
             * Adds a package atom to uninstall.
             *
             * @param atom package atom (e.g., "name(version)")
             * @return this builder
             */
            public UninstallCommandBuilder atom(String atom) {
                atoms.add(atom);
                return this;
            }

            /**
             * Sets assume-yes flag.
             *
             * @param assumeYes if true, assume yes to prompts
             * @return this builder
             */
            public UninstallCommandBuilder assumeYes(boolean assumeYes) {
                this.assumeYes = assumeYes;
                return this;
            }

            /**
             * Finishes uninstall command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                parent.args(atoms);
                if (assumeYes) {
                    parent.args("-y");
                }
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for venv commands. Only contains Ruyi-specific options. Non-Ruyi settings (environment,
         * lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class VenvCommandBuilder {
            private final Builder parent;
            private String profile;
            private String dest;
            private String name;
            private String toolchain;
            private String emulator;
            private boolean withSysroot = true;
            private String sysrootFrom;
            private String extraCommandsFrom;

            private VenvCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("venv");
            }

            public VenvCommandBuilder profile(String profile) {
                this.profile = profile;
                return this;
            }

            public VenvCommandBuilder dest(String dest) {
                this.dest = dest;
                return this;
            }

            public VenvCommandBuilder name(String name) {
                this.name = name;
                return this;
            }

            public VenvCommandBuilder toolchain(String toolchain) {
                this.toolchain = toolchain;
                return this;
            }

            public VenvCommandBuilder emulator(String emulator) {
                this.emulator = emulator;
                return this;
            }

            public VenvCommandBuilder withSysroot(boolean withSysroot) {
                this.withSysroot = withSysroot;
                return this;
            }

            public VenvCommandBuilder sysrootFrom(String sysrootFrom) {
                this.sysrootFrom = sysrootFrom;
                return this;
            }

            public VenvCommandBuilder extraCommandsFrom(String extraCommandsFrom) {
                this.extraCommandsFrom = extraCommandsFrom;
                return this;
            }

            /**
             * Finishes venv command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (name != null && !name.isBlank()) {
                    parent.args("--name", name);
                }
                if (toolchain != null && !toolchain.isBlank()) {
                    parent.args("--toolchain", toolchain);
                }
                if (emulator != null && !emulator.isBlank()) {
                    parent.args("--emulator", emulator);
                }
                if (withSysroot) {
                    parent.args("--with-sysroot");
                } else {
                    parent.args("--without-sysroot");
                }
                if (sysrootFrom != null && !sysrootFrom.isBlank()) {
                    parent.args("--sysroot-from", sysrootFrom);
                }
                if (extraCommandsFrom != null && !extraCommandsFrom.isBlank()) {
                    parent.args("--extra-commands-from", extraCommandsFrom);
                }
                if (profile != null && !profile.isBlank()) {
                    parent.args(profile);
                }
                if (dest != null && !dest.isBlank()) {
                    parent.args(dest);
                }
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for config commands (get, set, unset, remove-section). Only contains Ruyi-specific
         * options. Non-Ruyi settings (environment, lineCallback, etc.) must be configured on the parent
         * Builder before calling this.
         */
        public static class ConfigCommandBuilder {
            private final Builder parent;
            private String subcommand;
            private String key;
            private String value;

            private ConfigCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("config");
            }

            /**
             * Gets a config value.
             *
             * @param key config key
             * @return this builder
             */
            public ConfigCommandBuilder get(String key) {
                this.subcommand = "get";
                this.key = key;
                return this;
            }

            /**
             * Sets a config value.
             *
             * @param key config key
             * @param value config value
             * @return this builder
             */
            public ConfigCommandBuilder set(String key, String value) {
                this.subcommand = "set";
                this.key = key;
                this.value = value;
                return this;
            }

            /**
             * Unsets a config value.
             *
             * @param key config key
             * @return this builder
             */
            public ConfigCommandBuilder unset(String key) {
                this.subcommand = "unset";
                this.key = key;
                return this;
            }

            /**
             * Removes a config section.
             *
             * @param section section name
             * @return this builder
             */
            public ConfigCommandBuilder removeSection(String section) {
                this.subcommand = "remove-section";
                this.key = section;
                return this;
            }

            /**
             * Finishes config command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (subcommand != null) {
                    parent.args(subcommand);
                    if (key != null) {
                        parent.args(key);
                    }
                    if (value != null) {
                        parent.args(value);
                    }
                }
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for telemetry commands. Only contains Ruyi-specific options. Non-Ruyi settings
         * (environment, lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class TelemetryCommandBuilder {
            private final Builder parent;
            private String subcommand;

            private TelemetryCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("telemetry");
            }

            /**
             * Gives consent to telemetry.
             *
             * @return this builder
             */
            public TelemetryCommandBuilder consent() {
                this.subcommand = "consent";
                return this;
            }

            /**
             * Sets telemetry to local collection only.
             *
             * @return this builder
             */
            public TelemetryCommandBuilder local() {
                this.subcommand = "local";
                return this;
            }

            /**
             * Opts out of telemetry.
             *
             * @return this builder
             */
            public TelemetryCommandBuilder optOut() {
                this.subcommand = "optout";
                return this;
            }

            /**
             * Gets telemetry status.
             *
             * @return this builder
             */
            public TelemetryCommandBuilder status() {
                this.subcommand = "status";
                return this;
            }

            /**
             * Uploads telemetry data.
             *
             * @return this builder
             */
            public TelemetryCommandBuilder upload() {
                this.subcommand = "upload";
                return this;
            }

            /**
             * Finishes telemetry command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (subcommand != null) {
                    parent.args(subcommand);
                }
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for update commands. Only contains Ruyi-specific options. Non-Ruyi settings (environment,
         * lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class UpdateCommandBuilder {
            private final Builder parent;

            private UpdateCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("update");
            }

            /**
             * Finishes update command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }

        /**
         * Builder for entity commands. Only contains Ruyi-specific options. Non-Ruyi settings (environment,
         * lineCallback, etc.) must be configured on the parent Builder before calling this.
         */
        public static class EntityCommandBuilder {
            private final Builder parent;
            private String subcommand;
            private final List<String> subArgs = new ArrayList<>();

            private EntityCommandBuilder(Builder parent) {
                this.parent = parent;
                parent.command("entity");
            }

            /**
             * Lists entities.
             *
             * @param type entity type
             * @return this builder
             */
            public EntityCommandBuilder list(String type) {
                this.subcommand = "list";
                subArgs.add("-t");
                subArgs.add(type);
                return this;
            }

            /**
             * Finishes entity command configuration and returns to parent builder.
             *
             * @return parent builder
             */
            public Builder end() {
                if (subcommand != null) {
                    parent.args(subcommand);
                    parent.args(subArgs);
                }
                return parent;
            }

            /**
             * Executes the command.
             *
             * @return execution result
             * @throws RuyiCliException if the command fails
             */
            public RuyiExecResult execute() throws RuyiCliException {
                return end().build().execute();
            }
        }
    }
}
