package org.ruyisdk.venv.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.core.exception.RuyiConfigException;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.services.RuyiCli;
import org.ruyisdk.venv.Activator;

/** Service facade for listing and managing Ruyi virtual environments. */
public class VenvDetectionService {
    private static final String VENV_CONFIG_FILE_NAME = "ruyi-venv.toml";
    private static final PluginLogger LOGGER = Activator.getLogger();

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
            if (p == null || p.isBlank()) {
                continue;
            }
            out.add(p);
        }
        return List.copyOf(out);
    }

    private static List<Path> toPathList(List<String> pathStrings) {
        if (pathStrings == null || pathStrings.isEmpty()) {
            return List.of();
        }
        final var out = new ArrayList<Path>();
        for (final var p : pathStrings) {
            if (p == null || p.isBlank()) {
                continue;
            }
            out.add(Path.of(p));
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    /** Returns the list of known profiles from the Ruyi CLI. */
    public List<RuyiCli.ProfileInfo> listProfiles() {
        return RuyiCli.listProfiles();
    }

    /** Returns the list of known toolchains from the Ruyi CLI. */
    public List<RuyiCli.ToolchainInfo> listToolchains() {
        return RuyiCli.listToolchains();
    }

    /** Returns the list of known emulators from the Ruyi CLI. */
    public List<RuyiCli.EmulatorInfo> listEmulators() {
        return RuyiCli.listEmulators();
    }

    /** Installs a package via the Ruyi CLI. */
    public void installPackage(String name, String version) {
        LOGGER.logInfo("Installing package: name=" + name + ", version=" + version);
        RuyiCli.installPackage(name, version);
        LOGGER.logInfo("Package install finished: name=" + name + ", version=" + version);
    }

    /** Creates a new venv via the Ruyi CLI. */
    public void createVenv(String path, String toolchainName, String toolchainVersion,
            String profile, String emulatorName, String emulatorVersion) {
        LOGGER.logInfo(String.format(
                "Creating venv: path=%s, profile=%s, toolchain=%s:%s, emulator=%s:%s", path,
                profile, toolchainName, toolchainVersion, emulatorName, emulatorVersion));
        RuyiCli.createVenv(path, toolchainName, toolchainVersion, profile, emulatorName,
                emulatorVersion);
        refreshWorkspaceProjects(null);
        LOGGER.logInfo("Venv creation finished: path=" + path);
    }

    private static List<Venv> detectProjectVenvs(List<Path> projectPaths) {
        if (projectPaths == null || projectPaths.isEmpty()) {
            return List.of();
        }

        LOGGER.logInfo("Detecting project venvs: openProjects=" + projectPaths.size());
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
                    final var venv = Venv.createForProject(childDir.toString(), cfg.profile,
                            cfg.sysroot, projectPath.toString());

                    // Derive toolchain path and prefix from venv's bin directory
                    final var toolchainInfo = deriveToolchainInfo(childDir);
                    venv.setToolchainPath(toolchainInfo.binPath);
                    venv.setToolchainPrefix(toolchainInfo.prefix);

                    out.add(venv);
                });
            } catch (IOException e) {
                // ignore per-project failures
                LOGGER.logError("Failed to scan project for venv config: path=" + projectPath, e);
            }
        }
        LOGGER.logInfo("Project venv detection completed: count=" + out.size());
        return out;
    }

    /**
     * Detects project venvs asynchronously for the provided project roots and passes them to the
     * callback.
     */
    public void detectProjectVenvsAsync(List<String> projectRootPaths,
            Consumer<List<Venv>> callback) {
        final var detectJob = Job.create("Detecting virtual environments", monitor -> {
            LOGGER.logInfo("Detecting project venvs (async)");

            try {
                final var projectPaths = toPathList(projectRootPaths);
                final var result = detectProjectVenvs(projectPaths);
                LOGGER.logInfo("Project venv detection finished: count=" + result.size());
                callback.accept(result);
                return Status.OK_STATUS;
            } catch (Exception e) {
                return Status.error("Failed to detect project venvs", e);
            }
        });
        detectJob.schedule();
    }

    /**
     * Deletes venv directories asynchronously and reports completion or errors through the
     * callback.
     */
    public void deleteVenvDirectoriesAsync(List<String> venvDirectoryPaths,
            Consumer<Exception> callback) {
        final var deleteJob = Job.create("Deleting virtual environments", monitor -> {
            LOGGER.logInfo("Deleting venv directories: count="
                    + (venvDirectoryPaths == null ? 0 : venvDirectoryPaths.size()));
            try {
                final var venvDirectories = toPathList(venvDirectoryPaths);
                if (venvDirectories != null) {
                    for (final var dir : venvDirectories) {
                        LOGGER.logInfo("Deleting venv directory: path=" + dir);
                        deleteDirectoryRecursively(dir);
                    }

                    refreshWorkspaceProjects(monitor);
                }
                LOGGER.logInfo("Venv directories deletion finished");
                callback.accept(null);
                return Status.OK_STATUS;
            } catch (Exception e) {
                callback.accept(e);
                return Status.CANCEL_STATUS; // avoid Eclipse error dialog
            }
        });
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

    private static final class DerivedToolchainInfo {
        private final String binPath;
        private final String prefix;

        private DerivedToolchainInfo(String binPath, String prefix) {
            this.binPath = binPath == null ? "" : binPath;
            this.prefix = prefix == null ? "" : prefix;
        }
    }

    /**
     * Derives toolchain bin path and prefix by scanning the venv's bin directory for a GCC
     * executable.
     */
    private static DerivedToolchainInfo deriveToolchainInfo(Path venvPath) {
        if (venvPath == null) {
            return new DerivedToolchainInfo("", "");
        }
        final var binDir = venvPath.resolve("bin");
        if (!Files.isDirectory(binDir)) {
            return new DerivedToolchainInfo("", "");
        }
        try (Stream<Path> entries = Files.list(binDir)) {
            for (final var entry : (Iterable<Path>) entries::iterator) {
                if (entry == null) {
                    continue;
                }
                final var fileName = entry.getFileName();
                if (fileName == null) {
                    continue;
                }
                final var name = fileName.toString();
                // Look for *-gcc executable
                if (name.endsWith("-gcc") && Files.isRegularFile(entry)) {
                    final var prefix = name.substring(0, name.length() - "-gcc".length());
                    return new DerivedToolchainInfo(binDir.toString(), prefix);
                }
            }
        } catch (IOException e) {
            // ignore scan failures
            LOGGER.logError("Failed to list entries in dir: path=" + binDir, e);
        }
        // No GCC found, but bin directory exists
        return new DerivedToolchainInfo(binDir.toString(), "");
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
        } catch (IOException e) {
            // ignore parse failures
            LOGGER.logWarning("Failed to parse venv config: path=" + tomlPath, e);
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

    private static void deleteDirectoryRecursively(Path dir) {
        if (dir == null) {
            return;
        }
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                /** {@inheritDoc} */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                /** {@inheritDoc} */
                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exc)
                        throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw RuyiConfigException.deleteFailed(dir.toString(), e);
        }
    }

    private static void refreshWorkspaceProjects(IProgressMonitor monitor) {
        for (final var project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            try {
                project.refreshLocal(IResource.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                LOGGER.logError("Failed to refresh project: " + project.getName(), e);
            }
        }
    }
}
