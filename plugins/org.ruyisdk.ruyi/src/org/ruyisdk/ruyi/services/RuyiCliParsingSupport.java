package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Shared parsing helpers for ruyi CLI output.
 */
final class RuyiCliParsingSupport {
    private static final Pattern PROFILE_NAME_PATTERN =
            Pattern.compile("^\\s*([^\\s(]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROFILE_QUIRKS_PATTERN =
            Pattern.compile("needs quirks:\\s*([^)]*)", Pattern.CASE_INSENSITIVE);

    private RuyiCliParsingSupport() {}

    static List<RuyiCli.ProfileInfo> parseProfilesFromString(String input) {
        return parseProfilesFromPlainText(input);
    }

    static List<RuyiCli.NewsListItemInfo> parseNewsListFromString(String input) {
        final var out = new ArrayList<RuyiCli.NewsListItemInfo>();
        if (input == null || input.isBlank()) {
            return out;
        }

        for (final var o : parseJsonObjects(input)) {
            if (!isNewsItemObject(o)) {
                continue;
            }
            final var id = optStringOrNull(o, "id");
            final var ord = optIntegerOrNull(o, "ord");
            final var isRead = optBooleanOrNull(o, "is_read");
            final var title = chooseNewsDisplayTitle(o.optJSONArray("langs"));
            out.add(new RuyiCli.NewsListItemInfo(id, ord, isRead, title));
        }
        return out;
    }

    static RuyiCli.NewsReadResult parseNewsReadFromString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        final var objects = parseJsonObjects(input);
        if (objects.isEmpty()) {
            return null;
        }

        final var o = objects.get(0);
        final var id = optStringOrNull(o, "id");
        final var ord = optIntegerOrNull(o, "ord");
        final var isRead = optBooleanOrNull(o, "is_read");
        final var langs = o.optJSONArray("langs");
        final var title = chooseNewsDisplayTitle(langs);
        final var content = chooseNewsContent(langs);
        return new RuyiCli.NewsReadResult(id, ord, isRead, title, content);
    }

    static List<RuyiCli.PackageListEntryInfo> parsePackageListFromString(String input) {
        final var out = new ArrayList<RuyiCli.PackageListEntryInfo>();
        if (input == null || input.isBlank()) {
            return out;
        }

        for (final var o : parseJsonObjects(input)) {
            if (!isPackageListObject(o)) {
                continue;
            }

            final var category = optStringOrNull(o, "category");
            final var name = optStringOrNull(o, "name");
            if (category == null || name == null) {
                continue;
            }

            final var versions = extractPackageVersions(o.optJSONArray("vers"));
            out.add(new RuyiCli.PackageListEntryInfo(category, name, versions));
        }

        return out;
    }

    static List<RuyiCli.PackageTreeCategoryInfo> parsePackageTreeFromString(String input) {
        final var categories = new LinkedHashMap<String, List<RuyiCli.PackageTreePackageInfo>>();
        for (final var packageEntry : parsePackageListFromString(input)) {
            final var packages =
                    categories.computeIfAbsent(packageEntry.getCategory(), k -> new ArrayList<>());

            final var versions = new ArrayList<RuyiCli.PackageTreeVersionInfo>();
            for (final var version : packageEntry.getVersions()) {
                final var packageRef =
                        String.format("%s(%s)", packageEntry.getName(), version.getSemver());
                versions.add(new RuyiCli.PackageTreeVersionInfo(
                        formatVersionLabel(version.getSemver(), version.getRemark()), packageRef,
                        version.isInstalled()));
            }
            packages.add(new RuyiCli.PackageTreePackageInfo(packageEntry.getName(), versions));
        }

        final var out = new ArrayList<RuyiCli.PackageTreeCategoryInfo>();
        for (final var category : categories.entrySet()) {
            out.add(new RuyiCli.PackageTreeCategoryInfo(category.getKey(), category.getValue()));
        }
        return out;
    }

    static String findInstalledToolchainFromRelatedEntityOutput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        for (final var o : parseJsonObjects(input)) {
            if (!isPackageListObject(o)) {
                continue;
            }

            final var category = o.optString("category", "");
            if (!"toolchain".equalsIgnoreCase(category)) {
                continue;
            }

            final var installedToolchain = findInstalledToolchainInEntry(o);
            if (installedToolchain != null) {
                return installedToolchain;
            }
        }

        return null;
    }

    static List<RuyiCli.ToolchainInfo> parseToolchainsFromString(String input) {
        final var packages = parsePackageInfos(input, "toolchain", "toolchain");
        final var out = new ArrayList<RuyiCli.ToolchainInfo>();
        for (final var info : packages) {
            out.add(new RuyiCli.ToolchainInfo(info.name, info.versions, info.quirks));
        }
        return out;
    }

    static List<RuyiCli.EmulatorInfo> parseEmulatorsFromString(String input) {
        final var packages = parsePackageInfos(input, "emulator", "emulator");
        final var out = new ArrayList<RuyiCli.EmulatorInfo>();
        for (final var info : packages) {
            out.add(new RuyiCli.EmulatorInfo(info.name, info.versions, info.quirks));
        }
        return out;
    }

    private static List<RuyiCli.ProfileInfo> parseProfilesFromPlainText(String input) {
        final var out = new ArrayList<RuyiCli.ProfileInfo>();
        if (input == null || input.isBlank()) {
            return out;
        }

        try (var reader = new BufferedReader(new StringReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final var trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                final var nameMatcher = PROFILE_NAME_PATTERN.matcher(trimmed);
                if (!nameMatcher.find()) {
                    continue;
                }
                final var name = nameMatcher.group(1);

                final var quirks = new LinkedHashSet<String>();
                final var quirksMatcher = PROFILE_QUIRKS_PATTERN.matcher(trimmed);
                if (quirksMatcher.find()) {
                    var raw = quirksMatcher.group(1).trim();
                    if (raw.startsWith("{") && raw.endsWith("}")) {
                        raw = raw.substring(1, raw.length() - 1);
                    }
                    for (final var token : raw.split(",")) {
                        final var t = token.replace("'", "").replace("\"", "").trim();
                        if (!t.isEmpty()) {
                            quirks.add(t);
                        }
                    }
                }

                out.add(new RuyiCli.ProfileInfo(name, new ArrayList<>(quirks)));
            }
        } catch (IOException e) {
            throw RuyiCliException.ioError(e);
        }
        return out;
    }

    private static List<PackageInfo> parsePackageInfos(String input, String expectedCategory,
            String metadataKey) {
        final var out = new ArrayList<PackageInfo>();
        if (input == null || input.isBlank()) {
            return out;
        }

        for (final var o : parseJsonObjects(input)) {
            if (o == null) {
                continue;
            }

            if (!isPackageListObject(o)) {
                continue;
            }

            final var category = o.optString("category", "");
            if (!expectedCategory.equalsIgnoreCase(category)) {
                continue;
            }

            final var name = o.optString("name", "").trim();
            if (name == null || name.isBlank()) {
                continue;
            }

            final var versions = extractVersions(o.optJSONArray("vers"));
            if (versions.isEmpty()) {
                continue;
            }

            final var quirks = extractPackageQuirks(o.optJSONArray("vers"), metadataKey);
            out.add(new PackageInfo(name.trim(), versions, quirks));
        }
        return out;
    }

    private static List<RuyiCli.PackageVersionInfo> extractPackageVersions(
            JSONArray versionsArray) {
        final var out = new ArrayList<RuyiCli.PackageVersionInfo>();
        if (versionsArray == null) {
            return out;
        }

        for (int i = 0; i < versionsArray.length(); i++) {
            final var versionObj = versionsArray.optJSONObject(i);
            if (versionObj == null) {
                continue;
            }

            final var semver = optStringOrNull(versionObj, "semver");
            if (semver == null) {
                continue;
            }

            final var remark = extractPrimaryRemark(versionObj.optJSONArray("remarks"));
            final var isInstalled =
                    Boolean.TRUE.equals(optBooleanOrNull(versionObj, "is_installed"));
            out.add(new RuyiCli.PackageVersionInfo(semver, remark, isInstalled));
        }

        return out;
    }

    private static String extractPrimaryRemark(JSONArray remarksArray) {
        if (remarksArray == null || remarksArray.length() == 0) {
            return null;
        }

        final var remark = remarksArray.optString(0, "").trim();
        return remark.isEmpty() ? null : remark;
    }

    private static String formatVersionLabel(String semver, String remark) {
        if (remark == null || remark.isBlank()) {
            return semver;
        }
        return semver + " [" + remark + "]";
    }

    private static List<String> extractVersions(JSONArray versionsArray) {
        final var versionSet = new LinkedHashSet<String>();
        if (versionsArray == null) {
            return new ArrayList<>(versionSet);
        }

        for (int i = 0; i < versionsArray.length(); i++) {
            final var versionObj = versionsArray.optJSONObject(i);
            if (versionObj == null) {
                continue;
            }
            final var version = versionObj.optString("semver", "").trim();
            if (!version.isEmpty()) {
                versionSet.add(version);
            }
        }
        return new ArrayList<>(versionSet);
    }

    private static List<String> extractPackageQuirks(JSONArray vers, String metadataKey) {
        final var quirksSet = new LinkedHashSet<String>();
        if (vers == null || vers.length() == 0) {
            return new ArrayList<>(quirksSet);
        }

        for (int i = 0; i < vers.length(); i++) {
            final var v = vers.optJSONObject(i);
            if (v == null) {
                continue;
            }

            final var pm = v.optJSONObject("pm");
            if (pm == null) {
                continue;
            }

            final var metadata = pm.optJSONObject(metadataKey);
            if (metadata == null) {
                continue;
            }

            collectJsonArrayStrings(metadata.optJSONArray("quirks"), quirksSet);
            collectJsonArrayStrings(metadata.optJSONArray("flavors"), quirksSet);
            break;
        }

        return new ArrayList<>(quirksSet);
    }

    private static void collectJsonArrayStrings(JSONArray arr, LinkedHashSet<String> target) {
        if (arr == null) {
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            final var value = arr.optString(i, "").trim();
            if (!value.isEmpty()) {
                target.add(value);
            }
        }
    }

    private static String chooseNewsDisplayTitle(JSONArray langs) {
        final var best = chooseBestNewsLang(langs);
        if (best == null) {
            return null;
        }
        return optStringOrNull(best, "display_title");
    }

    private static String chooseNewsContent(JSONArray langs) {
        final var best = chooseBestNewsLang(langs);
        if (best == null) {
            return null;
        }
        return optStringOrNull(best, "content");
    }

    private static JSONObject chooseBestNewsLang(JSONArray langs) {
        if (langs == null || langs.length() == 0) {
            return null;
        }

        JSONObject first = null;
        final var locale =
                Locale.getDefault() == null ? "" : normalizeLang(Locale.getDefault().toString());

        for (int i = 0; i < langs.length(); i++) {
            final var candidate = langs.optJSONObject(i);
            if (candidate == null) {
                continue;
            }
            if (first == null) {
                first = candidate;
            }
            final var lang = normalizeLang(candidate.optString("lang", ""));
            if ("en_US".equalsIgnoreCase(lang)) {
                return candidate;
            }
        }

        if (!locale.isBlank()) {
            for (int i = 0; i < langs.length(); i++) {
                final var candidate = langs.optJSONObject(i);
                if (candidate == null) {
                    continue;
                }

                final var lang = normalizeLang(candidate.optString("lang", ""));
                if (lang.equalsIgnoreCase(locale)) {
                    return candidate;
                }
                if (lang.length() >= 2 && locale.length() >= 2
                        && lang.substring(0, 2).equalsIgnoreCase(locale.substring(0, 2))) {
                    return candidate;
                }
            }
        }

        return first;
    }

    private static String normalizeLang(String lang) {
        if (lang == null) {
            return "";
        }
        return lang.trim().replace('-', '_');
    }

    private static String findInstalledToolchainInEntry(JSONObject packageObject) {
        final var versions = packageObject.optJSONArray("vers");
        if (versions == null) {
            return null;
        }

        final var packageName = optStringOrNull(packageObject, "name");
        for (int i = 0; i < versions.length(); i++) {
            final var versionObject = versions.optJSONObject(i);
            if (versionObject == null
                    || !Boolean.TRUE.equals(optBooleanOrNull(versionObject, "is_installed"))) {
                continue;
            }

            final var fromInstallCommand = extractPackageRefFromInstallCommand(
                    optStringOrNull(versionObject, "install_command"));
            if (fromInstallCommand != null) {
                return fromInstallCommand;
            }

            final var semver = optStringOrNull(versionObject, "semver");
            if (packageName != null && semver != null) {
                return String.format("%s(%s)", packageName, semver);
            }
        }

        return null;
    }

    private static String extractPackageRefFromInstallCommand(String installCommand) {
        if (installCommand == null || installCommand.isBlank()) {
            return null;
        }

        final var trimmed = installCommand.trim();
        final var lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace < 0 || lastSpace + 1 >= trimmed.length()) {
            return null;
        }

        return trimmed.substring(lastSpace + 1);
    }

    private static boolean isPackageListObject(JSONObject o) {
        final var type = o.optString("ty", "").trim();
        if (type.isEmpty()) {
            return optStringOrNull(o, "category") != null && optStringOrNull(o, "name") != null;
        }
        return "pkglistoutput-v1".equalsIgnoreCase(type);
    }

    private static boolean isNewsItemObject(JSONObject o) {
        final var type = o.optString("ty", "");
        return "newsitem-v1".equalsIgnoreCase(type);
    }

    private static List<JSONObject> parseJsonObjects(String input) {
        final var out = new ArrayList<JSONObject>();
        for (final var json : extractJsonObjects(input)) {
            try {
                out.add(new JSONObject(json));
            } catch (RuntimeException e) {
                // Skip malformed/truncated objects.
            }
        }
        return out;
    }

    private static List<String> extractJsonObjects(String input) {
        final var out = new ArrayList<String>();
        if (input == null || input.isBlank()) {
            return out;
        }

        int depth = 0;
        int start = -1;
        for (int i = 0; i < input.length(); i++) {
            final var c = input.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                if (depth == 0) {
                    continue;
                }
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(input.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return out;
    }

    private static String optStringOrNull(JSONObject o, String key) {
        if (!o.has(key)) {
            return null;
        }
        final var value = o.optString(key, null);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static Integer optIntegerOrNull(JSONObject o, String key) {
        if (!o.has(key)) {
            return null;
        }

        final var raw = o.opt(key);
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof String s) {
            final var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private static Boolean optBooleanOrNull(JSONObject o, String key) {
        if (!o.has(key)) {
            return null;
        }

        final var raw = o.opt(key);
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Number n) {
            return n.intValue() != 0;
        }
        if (raw instanceof String s) {
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return Boolean.FALSE;
            }
        }

        return null;
    }

    private static final class PackageInfo {
        private final String name;
        private final List<String> versions;
        private final List<String> quirks;

        private PackageInfo(String name, List<String> versions, List<String> quirks) {
            this.name = name;
            this.versions = versions;
            this.quirks = quirks;
        }
    }
}
