package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Simple DTOs returned by the CLI wrapper to avoid cross-plugin model coupling.
 */
public class RuyiCli {
    /** Profile information returned by the ruyi CLI. */
    public static class ProfileInfo {
        private final String name;
        private final String quirks;

        /** Creates an instance. */
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

        /** Creates an instance. */
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

        /** Creates an instance. */
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

    /** Virtual environment information returned by the ruyi CLI. */
    public static class VenvInfo {
        private final String path;
        private final String profile;
        private final String sysroot;
        private final Boolean activated;

        /** Creates an instance. */
        public VenvInfo(String path, String profile, String sysroot, Boolean activated) {
            this.path = path;
            this.profile = profile;
            this.sysroot = sysroot;
            this.activated = activated;
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

        public Boolean getActivated() {
            return activated;
        }
    }

    /** Summary information for a news item returned by the ruyi CLI. */
    public static class NewsListItemInfo {
        private final String id;
        private final Integer ord;
        private final Boolean read;
        private final String title;

        /** Creates an instance. */
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

        /** Creates an instance. */
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

    /* Minimal wrapper around the `ruyi` CLI (primarily porcelain output). */
    // All calls to the `ruyi` executable use the canonical install path
    // returned by `RuyiFileUtils.getInstallPath()`. Process creation is
    // centralized in `runRuyi(...)` below.

    /** Lists available profiles as reported by the ruyi CLI. */
    public static List<ProfileInfo> listProfiles() {
        final var out = new ArrayList<ProfileInfo>();
        try {
            var result = runRuyi(Arrays.asList("--porcelain", "list", "profiles"));
            var output = result.getOutput();
            final var exit = result.getExitCode();
            if (exit != 0 && (output == null || output.isEmpty())) {
                // fallback to non-porcelain
                result = runRuyi(Arrays.asList("list", "profiles"));
                output = result.getOutput();
            }

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
            final var args = new ArrayList<String>();
            args.add("--porcelain");
            args.add("news");
            args.add("list");
            if (onlyUnread) {
                args.add("--new");
            }

            final var result = runRuyi(args);
            return parseNewsListFromString(result.getOutput());
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    /** Reads a news item by ID or ordinal using the ruyi CLI. */
    public static NewsReadResult readNewsItem(String idOrOrd) {
        try {
            if (idOrOrd == null || idOrOrd.isBlank()) {
                return null;
            }
            final var args = Arrays.asList("--porcelain", "news", "read", "--quiet", idOrOrd);
            final var result = runRuyi(args);
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

    private static String readAll(Process p) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            final var sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) {
                sb.append(l).append('\n');
            }
            return sb.toString();
        }
    }

    // Centralized process invocation for ruyi. Uses only the canonical
    // installation directory provided by RuyiFileUtils.getInstallPath(). If
    // no install path is available this returns exit -1 with a message.
    private static RunResult runRuyi(List<String> args) {
        String install = null;
        try {
            install = RuyiFileUtils.getInstallPath();
        } catch (Exception e) {
            // fall through to return empty result
        }
        if (install == null || install.isBlank()) {
            final var msg = "Ruyi install path not configured (RuyiFileUtils.getInstallPath() returned empty)";
            return new RunResult(-1, msg);
        }
        try {
            final var exe = install + File.separator + "ruyi";
            final var cmd = new ArrayList<String>();
            cmd.add(exe);
            if (args != null && !args.isEmpty()) {
                cmd.addAll(args);
            }
            final var pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            final var p = pb.start();
            final var rawOutput = readAll(p);
            final var exit = p.waitFor();
            // Build a readable command string (quote args containing spaces)
            final var cmdStrBuilder = new StringBuilder();
            for (int i = 0; i < cmd.size(); i++) {
                final var part = cmd.get(i);
                if (part.contains(" ")) {
                    cmdStrBuilder.append('\'').append(part).append('\'');
                } else {
                    cmdStrBuilder.append(part);
                }
                if (i < cmd.size() - 1) {
                    cmdStrBuilder.append(' ');
                }
            }
            final var cmdString = cmdStrBuilder.toString();
            final var output = rawOutput == null ? "" : rawOutput;
            if (exit != 0) {
                final var hint = "\nCommand: " + cmdString + "\n";
                return new RunResult(exit, output + hint);
            }
            return new RunResult(exit, output);
        } catch (IOException | InterruptedException e) {
            // If process start fails, include the attempted command for debugging
            final var cmdStrBuilder = new StringBuilder();
            if (args != null) {
                cmdStrBuilder.append(install == null ? "ruyi" : install + File.separator + "ruyi");
                for (final var a : args) {
                    cmdStrBuilder.append(' ');
                    if (a.contains(" ")) {
                        cmdStrBuilder.append('\'').append(a).append('\'');
                    } else {
                        cmdStrBuilder.append(a);
                    }
                }
            }
            final var cmdString = cmdStrBuilder.toString();
            final var out = e.getMessage() == null ? "" : e.getMessage();
            if (cmdString.isEmpty()) {
                return new RunResult(-1, out);
            }
            return new RunResult(-1, out + "\nCommand: " + cmdString + "\n");
        }
    }

    /** Result of a ruyi CLI invocation. */
    public static class RunResult {
        private final int exitCode;
        private final String output;

        /** Creates an instance. */
        public RunResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }
    }

    /**
     * Public wrapper to execute arbitrary ruyi arguments.
     *
     * @param args arguments to pass to the ruyi CLI
     * @return exit code and captured output
     */
    public static RunResult run(List<String> args) {
        return runRuyi(args);
    }

    /**
     * Run a repository index update (`ruyi update`).
     */
    public static RunResult update() {
        return run(Arrays.asList("--porcelain", "update"));
    }

    /**
     * Install a package by name and version (semver). This centralizes construction of the CLI
     * arguments so callers don't build parameter strings themselves.
     */
    public static RunResult installPackage(String name, String version) {
        if (name == null || name.isBlank() || version == null || version.isBlank()) {
            return new RunResult(-1, "Invalid package name or version");
        }
        // Use the documented package atom syntax: name(version)
        // Example: ruyi install 'gnu-upstream(0.20231118.0)'
        final var atom = name + "(" + version + ")";
        final var args = Arrays.asList("--porcelain", "install", atom);
        final var result = run(args);
        return result;
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
     * @return RunResult with exit code and captured output
     */
    public static RunResult createVenv(String path, String toolchainName, String toolchainVersion, String profile,
                    String emulatorName, String emulatorVersion) {
        if (path == null || path.isBlank()) {
            return new RunResult(-1, "Empty venv path");
        }
        if (toolchainName == null || toolchainName.isBlank() || toolchainVersion == null
                        || toolchainVersion.isBlank()) {
            return new RunResult(-1, "Invalid toolchain");
        }
        if (profile == null || profile.isBlank()) {
            return new RunResult(-1, "Invalid profile");
        }
        final var args = new ArrayList<String>();
        args.add("--porcelain");
        args.add("venv");
        // Pass toolchain/emulator atoms using the documented parentheses syntax
        args.add("--toolchain");
        args.add(toolchainName + "(" + toolchainVersion + ")");
        if (emulatorName != null && !emulatorName.isBlank() && emulatorVersion != null && !emulatorVersion.isBlank()) {
            args.add("--emulator");
            args.add(emulatorName + "(" + emulatorVersion + ")");
        }
        args.add(profile);
        args.add(path);
        return run(args);
    }

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
            final var result = runRuyi(
                            Arrays.asList("--porcelain", "list", "--category-is", "toolchain", "--name-contains", ""));
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
            final var result = runRuyi(
                            Arrays.asList("--porcelain", "list", "--category-is", "emulator", "--name-contains", ""));
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

    /**
     * Attempts to discover existing Ruyi virtual environments on the system. Heuristic: scan user's
     * home directory for folders that look like venvs (contain a "bin/activate" script) and return
     * simple VenvInfo entries.
     */
    public static List<VenvInfo> listVenvs() {
        final var out = new ArrayList<VenvInfo>();
        try {
            final var home = System.getProperty("user.home");
            if (home == null) {
                return out;
            }
            final var homeDir = new File(home);
            final var children = homeDir.listFiles();
            if (children == null) {
                return out;
            }
            for (final var dir : children) {
                if (!dir.isDirectory()) {
                    continue;
                }
                // common marker for venvs: bin/activate (POSIX) or Scripts/activate (Windows)
                final var binActivate = new File(dir, "bin/activate");
                final var scriptsActivate = new File(dir, "Scripts/activate");
                if (binActivate.exists() || scriptsActivate.exists()) {
                    final var path = dir.getAbsolutePath();
                    final var profile = dir.getName();
                    final var sysroot = new File(dir, "sysroot").getAbsolutePath();
                    out.add(new VenvInfo(path, profile, sysroot, Boolean.TRUE));
                }
                // also consider directories named *-venv or ruyiVenv
                if (dir.getName().toLowerCase().endsWith("-venv") || dir.getName().equalsIgnoreCase("ruyivenv")) {
                    final var path = dir.getAbsolutePath();
                    final var profile = dir.getName().replaceAll("-venv$", "");
                    final var sysroot = new File(dir, "sysroot").getAbsolutePath();
                    out.add(new VenvInfo(path, profile, sysroot, Boolean.FALSE));
                }
            }
        } catch (Exception e) {
            // ignore and return what we have
        }
        return out;
    }
}
