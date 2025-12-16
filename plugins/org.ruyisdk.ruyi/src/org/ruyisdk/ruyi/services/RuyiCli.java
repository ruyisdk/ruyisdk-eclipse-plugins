package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
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
        private final java.util.List<String> versions;

        /** Creates an instance. */
        public ToolchainInfo(String name, java.util.List<String> versions) {
            this.name = name;
            this.versions = versions == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(versions);
        }

        public String getName() {
            return name;
        }

        public java.util.List<String> getVersions() {
            return java.util.Collections.unmodifiableList(versions);
        }
    }

    /** Emulator package information returned by the ruyi CLI. */
    public static class EmulatorInfo {
        private final String name;
        private final java.util.List<String> versions;

        /** Creates an instance. */
        public EmulatorInfo(String name, java.util.List<String> versions) {
            this.name = name;
            this.versions = versions == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(versions);
        }

        public String getName() {
            return name;
        }

        public java.util.List<String> getVersions() {
            return java.util.Collections.unmodifiableList(versions);
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
        private final int ord;
        private final boolean read;
        private final String title;

        /** Creates an instance. */
        public NewsListItemInfo(String id, int ord, boolean read, String title) {
            this.id = id;
            this.ord = ord;
            this.read = read;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public int getOrd() {
            return ord;
        }

        public boolean isRead() {
            return read;
        }

        public String getTitle() {
            return title;
        }
    }

    /** Full content and metadata for a news item returned by the ruyi CLI. */
    public static class NewsReadResult {
        private final String id;
        private final int ord;
        private final boolean read;
        private final String title;
        private final String content;

        /** Creates an instance. */
        public NewsReadResult(String id, int ord, boolean read, String title, String content) {
            this.id = id;
            this.ord = ord;
            this.read = read;
            this.title = title;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public int getOrd() {
            return ord;
        }

        public boolean isRead() {
            return read;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

    /* Minimal wrapper around the `ruyi` CLI to fetch package lists. */
    // All calls to the `ruyi` executable use the canonical install path
    // returned by `RuyiFileUtils.getInstallPath()`. Process creation is
    // centralized in `runRuyi(...)` below.

    /** Lists available profiles as reported by the ruyi CLI. */
    public static List<ProfileInfo> listProfiles() {
        List<ProfileInfo> out = new ArrayList<>();
        try {
            RunResult res = runRuyi(Arrays.asList("--porcelain", "list", "profiles"));
            String all = res.getOutput();
            int exit = res.getExitCode();
            if (exit != 0 && (all == null || all.isEmpty())) {
                // fallback to non-porcelain
                res = runRuyi(Arrays.asList("list", "profiles"));
                all = res.getOutput();
            }

            if (all == null || all.isEmpty()) {
                return out;
            }

            String trimmed = all.trim();
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                // try parse JSON array/object
                try {
                    JSONArray arr = new JSONArray(trimmed);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String name = o.optString("name", o.optString("id", ""));
                        String quirks = o.optString("quirks", "");
                        out.add(new ProfileInfo(name, quirks));
                    }
                    return out;
                } catch (Exception e) {
                    // fallthrough to plain parsing
                }
            }

            // plain text parsing: lines like "wch-qingke-v2a (needs quirks: {'wch'})"
            // plain text parsing: lines like "wch-qingke-v2a (needs quirks: {'wch'})"
            Pattern namePtn = Pattern.compile("^\\s*([^\\s(]+)", Pattern.CASE_INSENSITIVE);
            Pattern quirksPtn = Pattern.compile("needs quirks:\\s*\\{([^}]*)\\}", Pattern.CASE_INSENSITIVE);
            BufferedReader r =
                            new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(all.getBytes())));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Matcher m = namePtn.matcher(line);
                String name = null;
                if (m.find()) {
                    name = m.group(1);
                }
                String quirks = "";
                Matcher q = quirksPtn.matcher(line);
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
            List<String> args = new ArrayList<>();
            args.add("--porcelain");
            args.add("news");
            args.add("list");
            if (onlyUnread) {
                args.add("--new");
            }

            RunResult res = runRuyi(args);
            return parseNewsListFromString(res.getOutput());
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    /** Reads a news item by ID or ordinal using the ruyi CLI. */
    public static NewsReadResult readNewsItem(String idOrOrd) {
        try {
            if (idOrOrd == null || idOrOrd.trim().isEmpty()) {
                return null;
            }
            List<String> args = Arrays.asList("--porcelain", "news", "read", "--quiet", idOrOrd);
            RunResult res = runRuyi(args);
            return parseNewsReadFromString(res.getOutput());
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses the output of a news list command. */
    public static List<NewsListItemInfo> parseNewsListFromString(String all) {
        List<NewsListItemInfo> out = new ArrayList<>();
        if (all == null || all.trim().isEmpty()) {
            return out;
        }
        try {
            for (JSONObject o : parseConcatenatedJsonObjects(all)) {
                if (o == null) {
                    continue;
                }
                String ty = o.optString("ty", "");
                if (!"newsitem-v1".equalsIgnoreCase(ty)) {
                    continue;
                }
                String id = o.optString("id", "").trim();
                int ord = o.optInt("ord", -1);
                boolean isRead = o.optBoolean("is_read", false);
                JSONArray langs = o.optJSONArray("langs");
                String title = chooseNewsDisplayTitle(langs);
                if (title == null || title.trim().isEmpty()) {
                    title = id;
                }
                if (id == null || id.isEmpty()) {
                    // Defensive: fall back to ord as string if id is absent.
                    id = ord >= 0 ? String.valueOf(ord) : "";
                }
                if (id != null && !id.isEmpty()) {
                    out.add(new NewsListItemInfo(id, ord, isRead, title));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /** Parses the output of a news read command. */
    public static NewsReadResult parseNewsReadFromString(String all) {
        if (all == null || all.trim().isEmpty()) {
            return null;
        }
        try {
            List<JSONObject> objs = parseConcatenatedJsonObjects(all);
            if (objs.isEmpty()) {
                return null;
            }
            JSONObject o = objs.get(0);
            if (o == null) {
                return null;
            }
            String id = o.optString("id", "").trim();
            int ord = o.optInt("ord", -1);
            boolean isRead = o.optBoolean("is_read", false);
            JSONArray langs = o.optJSONArray("langs");
            String title = chooseNewsDisplayTitle(langs);
            String content = chooseNewsContent(langs);
            return new NewsReadResult(id, ord, isRead, title, content);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<JSONObject> parseConcatenatedJsonObjects(String s) {
        List<JSONObject> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        try {
            JSONTokener t = new JSONTokener(s);
            while (true) {
                char c = t.nextClean();
                if (c == 0) {
                    break;
                }
                t.back();
                Object v = t.nextValue();
                if (v instanceof JSONObject) {
                    out.add((JSONObject) v);
                } else if (v instanceof JSONArray) {
                    JSONArray arr = (JSONArray) v;
                    for (int i = 0; i < arr.length(); i++) {
                        Object el = arr.get(i);
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
        JSONObject best = chooseBestNewsLang(langs);
        if (best == null) {
            return "";
        }
        return best.optString("display_title", "");
    }

    private static String chooseNewsContent(JSONArray langs) {
        JSONObject best = chooseBestNewsLang(langs);
        if (best == null) {
            return "";
        }
        return best.optString("content", "");
    }

    private static JSONObject chooseBestNewsLang(JSONArray langs) {
        if (langs == null || langs.length() == 0) {
            return null;
        }

        // Prefer an explicit English entry if available, else match current locale, else first.
        JSONObject first = null;
        String sysLang = Locale.getDefault() == null ? "" : Locale.getDefault().toString();
        for (int i = 0; i < langs.length(); i++) {
            JSONObject o = langs.optJSONObject(i);
            if (o == null) {
                continue;
            }
            if (first == null) {
                first = o;
            }
            String lang = o.optString("lang", "");
            if ("en_US".equalsIgnoreCase(lang)) {
                return o;
            }
        }
        if (!sysLang.isEmpty()) {
            for (int i = 0; i < langs.length(); i++) {
                JSONObject o = langs.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                String lang = o.optString("lang", "");
                if (sysLang.equalsIgnoreCase(lang)) {
                    return o;
                }
            }
        }
        return first;
    }

    private static String readAll(Process p) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = br.readLine()) != null) {
                sb.append(l).append('\n');
            }
            return sb.toString();
        }
    }

    // Centralized process invocation for ruyi. Uses only the canonical
    // installation directory provided by RuyiFileUtils.getInstallPath(). If
    // no install path is available this returns an empty output with exit -1.
    private static RunResult runRuyi(List<String> args) {
        String install = null;
        try {
            install = RuyiFileUtils.getInstallPath();
        } catch (Exception e) {
            // fall through to return empty result
        }
        if (install == null || install.trim().isEmpty()) {
            String msg = "Ruyi install path not configured (RuyiFileUtils.getInstallPath() returned empty)";
            return new RunResult(-1, msg);
        }
        try {
            String exe = install + File.separator + "ruyi";
            List<String> cmd = new ArrayList<>();
            cmd.add(exe);
            if (args != null && !args.isEmpty()) {
                cmd.addAll(args);
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String all = readAll(p);
            int exit = p.waitFor();
            // Build a readable command string (quote args containing spaces)
            StringBuilder cmdStrBuilder = new StringBuilder();
            for (int i = 0; i < cmd.size(); i++) {
                String part = cmd.get(i);
                if (part.contains(" ")) {
                    cmdStrBuilder.append('\'').append(part).append('\'');
                } else {
                    cmdStrBuilder.append(part);
                }
                if (i < cmd.size() - 1) {
                    cmdStrBuilder.append(' ');
                }
            }
            String cmdString = cmdStrBuilder.toString();
            String output = all == null ? "" : all;
            if (exit != 0) {
                String hint = "\nCommand: " + cmdString + "\n";
                return new RunResult(exit, output + hint);
            }
            return new RunResult(exit, output);
        } catch (IOException | InterruptedException e) {
            // If process start fails, include the attempted command for debugging
            StringBuilder cmdStrBuilder = new StringBuilder();
            if (args != null) {
                cmdStrBuilder.append(install == null ? "ruyi" : install + File.separator + "ruyi");
                for (String a : args) {
                    cmdStrBuilder.append(' ');
                    if (a.contains(" ")) {
                        cmdStrBuilder.append('\'').append(a).append('\'');
                    } else {
                        cmdStrBuilder.append(a);
                    }
                }
            }
            String cmdString = cmdStrBuilder.toString();
            String out = e.getMessage() == null ? "" : e.getMessage();
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
        if (name == null || name.trim().isEmpty() || version == null || version.trim().isEmpty()) {
            return new RunResult(-1, "Invalid package name or version");
        }
        // Use the documented package atom syntax: name(version)
        // Example: ruyi install 'gnu-upstream(0.20231118.0)'
        String atom = name + "(" + version + ")";
        List<String> args = Arrays.asList("--porcelain", "install", atom);
        RunResult res = run(args);
        return res;
    }

    /**
     * Create a virtual environment. All CLI argument construction stays here so callers pass structured
     * values only.
     *
     * @param path destination filesystem path for the venv (required)
     * @param toolchainName toolchain package name (required)
     * @param toolchainVersion toolchain version (required)
     * @param profile optional profile name (may be null)
     * @param emulatorName optional emulator package name (may be null)
     * @param emulatorVersion optional emulator version (may be null)
     * @return RunResult with exit code and captured output
     */
    public static RunResult createVenv(String path, String toolchainName, String toolchainVersion, String profile,
                    String emulatorName, String emulatorVersion) {
        if (path == null || path.trim().isEmpty()) {
            return new RunResult(-1, "Empty venv path");
        }
        if (toolchainName == null || toolchainName.trim().isEmpty() || toolchainVersion == null
                        || toolchainVersion.trim().isEmpty()) {
            return new RunResult(-1, "Invalid toolchain");
        }
        if (profile == null || profile.trim().isEmpty()) {
            return new RunResult(-1, "Invalid profile");
        }
        List<String> args = new ArrayList<>();
        args.add("--porcelain");
        args.add("venv");
        // Pass toolchain/emulator atoms using the documented parentheses syntax
        args.add("--toolchain");
        args.add(toolchainName + "(" + toolchainVersion + ")");
        if (emulatorName != null && !emulatorName.trim().isEmpty() && emulatorVersion != null
                        && !emulatorVersion.trim().isEmpty()) {
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
        List<String> objs = new ArrayList<>();
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

    // Placeholder implementations: attempt to call list and parse toolchains/emulators if present.
    /** Lists available toolchains as reported by the ruyi CLI. */
    public static List<ToolchainInfo> listToolchains() {
        List<ToolchainInfo> out = new ArrayList<>();
        try {
            RunResult res = runRuyi(
                            Arrays.asList("--porcelain", "list", "--category-is", "toolchain", "--name-contains", ""));
            String all = res.getOutput();
            if (all == null || all.isEmpty()) {
                return out;
            }
            out = parseToolchainsFromString(all);
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /**
     * Parse toolchain package objects from a porcelain output string (may contain concatenated JSON
     * objects). This helper is public to make parsing testable.
     */
    public static List<ToolchainInfo> parseToolchainsFromString(String all) {
        List<ToolchainInfo> out = new ArrayList<>();
        try {
            if (all == null || all.isEmpty()) {
                return out;
            }
            List<String> objs = extractJsonObjects(all);
            for (String jo : objs) {
                if (jo == null || jo.trim().isEmpty()) {
                    continue;
                }
                try {
                    JSONObject o = new JSONObject(jo);
                    String category = o.optString("category", "");
                    if (!"toolchain".equalsIgnoreCase(category)) {
                        continue;
                    }
                    String pkgName = o.optString("name", "").trim();
                    if (pkgName.isEmpty()) {
                        continue;
                    }
                    java.util.List<String> versions = new java.util.ArrayList<>();
                    JSONArray vers = o.optJSONArray("vers");
                    if (vers != null && vers.length() > 0) {
                        for (int vi = 0; vi < vers.length(); vi++) {
                            try {
                                JSONObject v = vers.optJSONObject(vi);
                                if (v == null) {
                                    continue;
                                }
                                String sem = v.optString("semver", v.optString("version", "")).trim();
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
        List<EmulatorInfo> out = new ArrayList<>();
        try {
            RunResult res = runRuyi(
                            Arrays.asList("--porcelain", "list", "--category-is", "emulator", "--name-contains", ""));
            String all = res.getOutput();
            if (all == null || all.isEmpty()) {
                return out;
            }
            out = parseEmulatorsFromString(all);
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /**
     * Parse emulator package objects from a porcelain output string (may contain concatenated JSON
     * objects). This helper is public to make parsing testable.
     */
    public static List<EmulatorInfo> parseEmulatorsFromString(String all) {
        List<EmulatorInfo> out = new ArrayList<>();
        try {
            if (all == null || all.isEmpty()) {
                return out;
            }
            List<String> objs = extractJsonObjects(all);
            for (String jo : objs) {
                if (jo == null || jo.trim().isEmpty()) {
                    continue;
                }
                try {
                    JSONObject o = new JSONObject(jo);
                    String category = o.optString("category", "");
                    if (!"emulator".equalsIgnoreCase(category)) {
                        continue;
                    }
                    String pkgName = o.optString("name", "").trim();
                    if (pkgName.isEmpty()) {
                        continue;
                    }
                    java.util.List<String> versions = new java.util.ArrayList<>();
                    JSONArray vers = o.optJSONArray("vers");
                    if (vers != null && vers.length() > 0) {
                        for (int vi = 0; vi < vers.length(); vi++) {
                            try {
                                JSONObject v = vers.optJSONObject(vi);
                                if (v == null) {
                                    continue;
                                }
                                String sem = v.optString("semver", v.optString("version", "")).trim();
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
        List<VenvInfo> out = new ArrayList<>();
        try {
            String home = System.getProperty("user.home");
            if (home == null) {
                return out;
            }
            java.io.File homeDir = new java.io.File(home);
            java.io.File[] children = homeDir.listFiles();
            if (children == null) {
                return out;
            }
            for (java.io.File f : children) {
                if (!f.isDirectory()) {
                    continue;
                }
                // common marker for venvs: bin/activate (POSIX) or Scripts/activate (Windows)
                java.io.File binActivate = new java.io.File(f, "bin/activate");
                java.io.File scriptsActivate = new java.io.File(f, "Scripts/activate");
                if (binActivate.exists() || scriptsActivate.exists()) {
                    String path = f.getAbsolutePath();
                    String profile = f.getName();
                    String sysroot = new java.io.File(f, "sysroot").getAbsolutePath();
                    out.add(new VenvInfo(path, profile, sysroot, Boolean.TRUE));
                }
                // also consider directories named *-venv or ruyiVenv
                if (f.getName().toLowerCase().endsWith("-venv") || f.getName().equalsIgnoreCase("ruyivenv")) {
                    String path = f.getAbsolutePath();
                    String profile = f.getName().replaceAll("-venv$", "");
                    String sysroot = new java.io.File(f, "sysroot").getAbsolutePath();
                    out.add(new VenvInfo(path, profile, sysroot, Boolean.FALSE));
                }
            }
        } catch (Exception e) {
            // ignore and return what we have
        }
        return out;
    }
}
