package org.ruyisdk.venv.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.ruyi.services.RuyiCli;
import org.ruyisdk.ruyi.util.RuyiLogger;
import org.ruyisdk.venv.Activator;

/** Service facade for listing and managing Ruyi virtual environments. */
public class VenvService {
    private static final String VENV_CONFIG_FILE_NAME = "ruyi-venv.toml";

    private static final RuyiLogger logger = Activator.getDefault().getLogger();

    private static List<Path> getOpenProjectPaths() {
        final var out = new ArrayList<Path>();
        try {
            final var root = ResourcesPlugin.getWorkspace().getRoot();
            for (var project : root.getProjects()) {
                if (project == null || !project.isOpen()) {
                    continue;
                }
                final var loc = project.getLocation();
                if (loc == null) {
                    continue;
                }
                out.add(Path.of(loc.toOSString()));
            }
        } catch (Exception e) {
            // ignore workspace discovery failures
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    /** Returns unique venv directory paths from the given venv list. */
    public List<String> getVenvDirectoryPathsFromVenvs(List<Venv> venvs) {
        if (venvs == null || venvs.isEmpty()) {
            return List.of();
        }
        final Set<String> out = new LinkedHashSet<>();
        for (final var venv : venvs) {
            if (venv == null) {
                continue;
            }
            final var p = venv.getPath();
            if (p == null) {
                continue;
            }
            final var trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            out.add(trimmed);
        }
        return List.copyOf(out);
    }

    private static List<Path> toPathList(List<String> pathStrings) {
        if (pathStrings == null || pathStrings.isEmpty()) {
            return List.of();
        }
        final var out = new ArrayList<Path>();
        for (final var p : pathStrings) {
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            out.add(Path.of(p.trim()));
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    /** Returns the list of known profiles from the Ruyi CLI. */
    public List<RuyiCli.ProfileInfo> listProfiles() {
        try {
            return RuyiCli.listProfiles();
        } catch (Exception e) {
            logger.logError("Failed to list profiles", e);
            return new ArrayList<>();
        }
    }

    /** Returns the list of known toolchains from the Ruyi CLI. */
    public List<RuyiCli.ToolchainInfo> listToolchains() {
        try {
            return RuyiCli.listToolchains();
        } catch (Exception e) {
            logger.logError("Failed to list toolchains", e);
            return new ArrayList<>();
        }
    }

    /** Returns the list of known emulators from the Ruyi CLI. */
    public List<RuyiCli.EmulatorInfo> listEmulators() {
        try {
            return RuyiCli.listEmulators();
        } catch (Exception e) {
            logger.logError("Failed to list emulators", e);
            return new ArrayList<>();
        }
    }

    private List<RuyiCli.VenvInfo> listVenvs() {
        try {
            return RuyiCli.listVenvs();
        } catch (Exception e) {
            logger.logError("Failed to list virtual environments", e);
            return new ArrayList<>();
        }
    }

    /** Updates the local package index via the Ruyi CLI. */
    public RuyiCli.RunResult updateIndex() {
        logger.logInfo("Updating Ruyi package index");
        try {
            final var result = RuyiCli.update();
            if (result != null) {
                logger.logInfo("Ruyi package index update finished: exit=" + result.getExitCode());
            }
            return result;
        } catch (Exception e) {
            logger.logError("Failed to update Ruyi package index", e);
            throw e;
        }
    }

    /** Installs a package via the Ruyi CLI. */
    public RuyiCli.RunResult installPackage(String name, String version) {
        logger.logInfo("Installing package: name=" + name + ", version=" + version);
        try {
            final var result = RuyiCli.installPackage(name, version);
            if (result != null) {
                logger.logInfo("Package install finished: name=" + name + ", version=" + version + ", exit="
                                + result.getExitCode());
            }
            return result;
        } catch (Exception e) {
            logger.logError("Failed to install package: name=" + name + ", version=" + version, e);
            throw e;
        }
    }

    /** Creates a new venv via the Ruyi CLI. */
    public RuyiCli.RunResult createVenv(String path, String toolchainName, String toolchainVersion, String profile,
                    String emulatorName, String emulatorVersion) {
        logger.logInfo("Creating venv: path=" + path + ", profile=" + profile + ", toolchain=" + toolchainName + ":"
                        + toolchainVersion + ", emulator=" + emulatorName + ":" + emulatorVersion);
        try {
            final var result = RuyiCli.createVenv(path, toolchainName, toolchainVersion, profile, emulatorName,
                            emulatorVersion);
            if (result != null) {
                logger.logInfo("Venv creation finished: path=" + path + ", exit=" + result.getExitCode());
            }
            return result;
        } catch (Exception e) {
            logger.logError("Failed to create venv: path=" + path, e);
            throw e;
        }
    }

    private List<Venv> fetchVenvs() {
        final var profileInfos = listProfiles();
        final Map<String, String> profileQuirks = new HashMap<>();
        for (final var profileInfo : profileInfos) {
            profileQuirks.put(profileInfo.getName(), profileInfo.getQuirks());
        }

        final var venvInfos = listVenvs();
        final var out = new ArrayList<Venv>();
        for (final var venvInfo : venvInfos) {
            final var quirks = profileQuirks.getOrDefault(venvInfo.getProfile(), "");
            out.add(new Venv(venvInfo.getPath(), venvInfo.getProfile(), venvInfo.getSysroot(), venvInfo.getActivated(),
                            quirks));
        }
        logger.logInfo("Fetched venv list: count=" + out.size());
        return out;
    }

    /** Fetches venvs asynchronously and passes them to the callback. */
    public void fetchVenvsAsync(Consumer<List<Venv>> callback) {
        final var fetchJob = new Job("Fetching virtual environments") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                logger.logInfo("Fetching venv list (async)");
                List<Venv> result;
                try {
                    result = fetchVenvs();
                } catch (Exception e) {
                    logger.logError("Failed to fetch venv list", e);
                    result = new ArrayList<>();
                }
                logger.logInfo("Venv list fetch completed: count=" + result.size());
                callback.accept(result);
                return Status.OK_STATUS;
            }
        };
        fetchJob.schedule();
    }

    private static List<Venv> detectProjectVenvs(List<Path> projectPaths) {
        if (projectPaths == null || projectPaths.isEmpty()) {
            return List.of();
        }

        logger.logInfo("Detecting project venvs: openProjects=" + projectPaths.size());
        final var out = new ArrayList<Venv>();
        for (final var projectPath : projectPaths) {
            if (projectPath == null) {
                continue;
            }
            if (!Files.isDirectory(projectPath)) {
                continue;
            }
            try (Stream<Path> children = Files.list(projectPath)) {
                children.filter(Objects::nonNull).filter(Files::isDirectory).forEach(childDir -> {
                    final var toml = childDir.resolve(VENV_CONFIG_FILE_NAME);
                    if (!Files.isRegularFile(toml)) {
                        return;
                    }
                    final var cfg = parseVenvConfigBestEffort(toml);
                    out.add(new Venv(childDir.toString(), cfg.profile, cfg.sysroot, projectPath.toString()));
                });
            } catch (Exception e) {
                logger.logError("Failed to scan project for venv config: path=" + projectPath, e);
                // ignore per-project failures
            }
        }
        logger.logInfo("Project venv detection completed: count=" + out.size());
        return out;
    }

    /** Detects project venvs asynchronously and passes them to the callback. */
    public void detectProjectVenvsAsync(Consumer<List<Venv>> callback) {
        final var detectJob = new Job("Detecting virtual environments") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                logger.logInfo("Detecting project venvs (async)");
                List<Venv> result;
                try {
                    final var projectPaths = getOpenProjectPaths();
                    result = detectProjectVenvs(projectPaths);
                } catch (Exception e) {
                    logger.logError("Failed to detect project venvs", e);
                    result = List.of();
                }
                logger.logInfo("Project venv detection finished: count=" + result.size());
                callback.accept(result);
                return Status.OK_STATUS;
            }
        };
        detectJob.schedule();
    }

    /** Deletes venv directories asynchronously and reports completion through the callback. */
    public void deleteVenvDirectoriesAsync(List<String> venvDirectoryPaths, Consumer<Exception> callback) {
        final var deleteJob = new Job("Deleting virtual environments") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                logger.logInfo("Deleting venv directories: count="
                                + (venvDirectoryPaths == null ? 0 : venvDirectoryPaths.size()));
                try {
                    final var venvDirectories = toPathList(venvDirectoryPaths);
                    if (venvDirectories != null) {
                        for (Path dir : venvDirectories) {
                            logger.logInfo("Deleting venv directory: path=" + dir);
                            deleteDirectoryRecursively(dir);
                        }
                    }
                    if (callback != null) {
                        callback.accept(null);
                    }
                    logger.logInfo("Venv directory deletion finished");
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    logger.logError("Failed to delete venv directories", e);
                    if (callback != null) {
                        callback.accept(e);
                    }
                    return new Status(IStatus.ERROR, "org.ruyisdk.venv", "Failed to delete virtual environment", e);
                }
            }
        };
        deleteJob.schedule();
    }

    private static final class DetectedVenvConfig {
        private final String profile;
        private final String sysroot;

        private DetectedVenvConfig(String profile, String sysroot) {
            this.profile = profile == null ? "" : profile;
            this.sysroot = sysroot == null ? "" : sysroot;
        }
    }

    private static DetectedVenvConfig parseVenvConfigBestEffort(Path tomlPath) {
        String profile = "";
        String sysroot = "";
        boolean inConfig = false;
        try {
            for (final var rawLine : Files.readAllLines(tomlPath, StandardCharsets.UTF_8)) {
                var line = rawLine == null ? "" : rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                final var commentIdx = line.indexOf('#');
                if (commentIdx >= 0) {
                    line = line.substring(0, commentIdx).trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    inConfig = "[config]".equals(line);
                    continue;
                }
                if (!inConfig) {
                    continue;
                }
                final var eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                final var key = line.substring(0, eq).trim();
                var val = line.substring(eq + 1).trim();
                val = unquote(val);
                if ("profile".equals(key)) {
                    profile = val;
                } else if ("sysroot".equals(key)) {
                    sysroot = val;
                }
            }
        } catch (Exception e) {
            logger.logWarning("Failed to parse venv config: path=" + tomlPath, e);
            // ignore parse failures
        }
        return new DetectedVenvConfig(profile, sysroot);
    }

    private static String unquote(String val) {
        if (val == null) {
            return "";
        }
        final var s = val.trim();
        if (s.length() >= 2) {
            final var first = s.charAt(0);
            final var last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private static void deleteDirectoryRecursively(Path dir) throws IOException {
        if (dir == null) {
            return;
        }
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            /** {@inheritDoc} */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            /** {@inheritDoc} */
            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
