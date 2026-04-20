package org.ruyisdk.projectcreator.builder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.ruyisdk.core.exception.PluginException;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.packages.JsonParser;
import org.ruyisdk.projectcreator.Activator;
import org.ruyisdk.projectcreator.RuyiProjectException;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * Builder for Makefile projects.
 */
public class MakefileBuilder extends IncrementalProjectBuilder {

    private static final PluginLogger LOGGER = Activator.getLogger();
    public static final String BUILDER_ID = "org.ruyisdk.projectcreator.makefileBuilder";
    private static final String BUILD_CMD_PROPERTY = "buildCmd";
    private static final String BOARD_MODEL_PROPERTY = "boardModel";
    private static final String OBJ_DIR_NAME = "obj";
    private static final String LOG_FILE_NAME = "build_output.log";
    private static final String RUYI_VENV_DIR = "ruyiVenv";
    public static final String RUYI_VENV_CMD_PROPERTY = "ruyiVenvCmd";

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();
        final File projectLocation = project.getLocation().toFile();

        // Create obj directory if it doesn't exist
        IFolder objFolder = project.getFolder(OBJ_DIR_NAME);
        if (!objFolder.exists()) {
            objFolder.create(true, true, monitor);
        }

        clearLogFile(project);
        logToFile(project, "=== Build started at " + new Date() + " ===");

        // 1. Ensure Ruyi virtual environment exists
        ensureRuyiVenv(project);

        // 2. Get the build command (e.g., "make")
        String buildCommand = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BUILD_CMD_PROPERTY));
        if (buildCommand == null || buildCommand.trim().isEmpty()) {
            buildCommand = "make";
        }

        logToFile(project, "Project: " + project.getName());

        // 3. Find the RISC-V GCC in the virtual environment
        File venvBinDir = new File(projectLocation, RUYI_VENV_DIR + "/bin");
        String gccPath = findRiscvGcc(venvBinDir);
        if (gccPath == null) {
            String errorMsg =
                            "Could not find RISC-V GCC in Ruyi virtual environment at: " + venvBinDir.getAbsolutePath();
            logToFile(project, errorMsg);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMsg));
        }

        logToFile(project, "Found RISC-V GCC at: " + gccPath);

        // Extract the prefix from the GCC path
        String gccName = new File(gccPath).getName();
        String prefix = gccName.replace("-gcc", "");
        logToFile(project, "Using toolchain prefix: " + prefix);

        // Determine the correct CFLAGS based on the toolchain
        String cflags = determineCorrectCflags(gccPath, project);
        logToFile(project, "Using CFLAGS: " + cflags);

        // 4. Execute make with explicit CC and OBJCOPY variables
        try {
            // Build the make command with explicit compiler paths and CFLAGS
            List<String> commands = new ArrayList<>();
            commands.add("bash");
            commands.add("-c");

            String makeCmd = String.format("cd %s && %s CC=%s/%s-gcc OBJCOPY=%s/%s-objcopy 'CFLAGS=%s'",
                            projectLocation.getAbsolutePath(), buildCommand, venvBinDir.getAbsolutePath(), prefix,
                            venvBinDir.getAbsolutePath(), prefix, cflags);

            commands.add(makeCmd);

            logToFile(project, "Build command: " + makeCmd);

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.logInfo("MakefileBuilder: " + line);
                    logToFile(project, line);
                }
            }
            int exitCode = process.waitFor();
            String resultMessage = "Build finished with exit code: " + exitCode;
            logToFile(project, resultMessage);

        } catch (IOException | InterruptedException e) {
            final var errorMessage = "Build failed with exception";
            LOGGER.logError("MakefileBuilder: " + errorMessage, e);
            logToFile(project, errorMessage + ": " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            logToFile(project, "=== Build finished at " + new Date() + " ===");
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            monitor.done();
        }

        return null;
    }

    /**
     * Determines the correct CFLAGS based on the toolchain type.
     *
     * @param gccPath The path to the gcc compiler
     * @param project The project
     * @return The appropriate CFLAGS for the toolchain
     */
    private String determineCorrectCflags(String gccPath, IProject project) {
        // Base CFLAGS that are always included
        String cflags = "-Wall -O2";

        // Get the board model to provide more specific flags
        String boardModel;
        try {
            boardModel = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BOARD_MODEL_PROPERTY));
        } catch (CoreException e) {
            throw RuyiProjectException.propertyAccessFailed(BOARD_MODEL_PROPERTY, e);
        }
        if (boardModel == null || boardModel.trim().isEmpty()) {
            return cflags; // Return default flags if board model is not available
        }

        // Analyze the GCC path to determine the type of toolchain
        if (gccPath.contains("unknown-linux-musl")) {
            // For Milk-V Duo with musl libc
            return cflags + " -march=rv64gc -mabi=lp64d";
        } else if (gccPath.contains("unknown-linux-gnu")) {
            // For GNU/Linux toolchains
            return cflags + " -march=rv64gc -mabi=lp64d";
        } else if (gccPath.contains("milkv-duo-elf")) {
            // Specific for Milk-V Duo bare metal
            return cflags + " -march=rv64gc -mabi=lp64d";
        } else if (gccPath.contains("unknown-elf") || gccPath.contains("plct-elf")) {
            // Generic bare metal toolchain
            if ("milkv-duo".equals(boardModel)) {
                return cflags + " -march=rv64gc -mabi=lp64d";
            } else {
                return cflags + " -march=rv64imac -mabi=lp64";
            }
        }

        // Default flags when we can't determine the specific toolchain type
        return cflags + " -march=rv64gc -mabi=lp64d";
    }

    /**
     * Searches for a RISC-V GCC compiler in the given directory.
     *
     * @param directory The directory to search in
     * @return The path to the RISC-V GCC, or null if not found
     */
    private String findRiscvGcc(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        // Look for files matching patterns like riscv64-*-gcc or riscv32-*-gcc
        File[] files = directory.listFiles(file -> {
            String name = file.getName();
            return (name.startsWith("riscv") || name.contains("-riscv")) && name.endsWith("-gcc");
        });

        if (files != null && files.length > 0) {
            return files[0].getAbsolutePath();
        }

        return null;
    }

    private void ensureRuyiVenv(IProject project) {
        File projectLocation = project.getLocation().toFile();
        File venvDir = new File(projectLocation, RUYI_VENV_DIR);

        if (venvDir.exists() && venvDir.isDirectory()) {
            logToFile(project, "Ruyi virtual environment found at: " + venvDir.getAbsolutePath());
            return; // Venv already exists, do nothing.
        }

        logToFile(project, "Ruyi virtual environment not found. Creating...");

        String venvCommand;
        try {
            venvCommand = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, RUYI_VENV_CMD_PROPERTY));
        } catch (CoreException e) {
            throw RuyiProjectException.propertyAccessFailed(RUYI_VENV_CMD_PROPERTY, e);
        }
        final var useCustomCommand = !(venvCommand == null || venvCommand.trim().isEmpty());
        String boardModel = null;
        String toolchain = null;
        List<String> ruyiArgs;

        if (!useCustomCommand) {
            logToFile(project, "No custom venv command found, generating default command...");
            try {
                boardModel = project
                                .getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BOARD_MODEL_PROPERTY));
            } catch (CoreException e) {
                throw RuyiProjectException.propertyAccessFailed(BOARD_MODEL_PROPERTY, e);
            }
            if (boardModel == null) {
                throw RuyiProjectException.propertyMissing(BOARD_MODEL_PROPERTY);
            }
            try {
                toolchain = JsonParser.findInstalledToolchainForBoard(boardModel);
            } catch (PluginException e) {
                logToFile(project, "Failed to find installed toolchain for board: " + e.getMessage());
                toolchain = null;
            }
            if (toolchain == null || toolchain.trim().isEmpty()) {
                if ("milkv-duo".equals(boardModel)) {
                    toolchain = "gnu-milkv-milkv-duo-elf-bin";
                } else if ("sipeed-lpi4a".equals(boardModel)) {
                    toolchain = "gnu-plct-xthead";
                } else {
                    toolchain = "gnu-plct";
                }
            } else {
                int versionIndex = toolchain.indexOf("-0.");
                if (versionIndex > 0) {
                    toolchain = toolchain.substring(0, versionIndex);
                }
            }
            ruyiArgs = List.of("venv", "-t", toolchain, boardModel, "./" + RUYI_VENV_DIR);
        } else {
            logToFile(project, "Using custom ruyi venv command from project properties.");
            // Strip leading "ruyi" token if present since runExperimental prepends it.
            List<String> tokens = List.of(DebugPlugin.parseArguments(venvCommand.trim()));
            ruyiArgs = (!tokens.isEmpty() && "ruyi".equals(tokens.get(0))) ? tokens.subList(1, tokens.size()) : tokens;
        }

        logToFile(project, "Executing: ruyi " + String.join(" ", ruyiArgs));

        final var output = RuyiCli.runRuyiExperimental(ruyiArgs, projectLocation);

        if (output != null && !output.isBlank()) {
            logToFile(project, "[ruyi venv] " + output);
        }
    }

    private void clearLogFile(IProject project) {
        IFolder objFolder = project.getFolder(OBJ_DIR_NAME);
        IFile logFile = objFolder.getFile(LOG_FILE_NAME);
        try {
            // Ensure obj folder exists
            if (!objFolder.exists()) {
                objFolder.create(IResource.FORCE, true, null);
            }
            if (logFile.exists()) {
                logFile.delete(true, null);
            }
        } catch (CoreException e) {
            LOGGER.logError("MakefileBuilder: Failed to clear log file", e);
        }
    }

    private void logToFile(IProject project, String message) {
        IFolder objFolder = project.getFolder(OBJ_DIR_NAME);
        IFile logFile = objFolder.getFile(LOG_FILE_NAME);
        try {
            if (!objFolder.exists()) {
                objFolder.create(IResource.FORCE, true, null);
            }
            if (!logFile.exists()) {
                try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
                    logFile.create(stream, true, null);
                }
            }
            try (FileWriter writer = new FileWriter(logFile.getLocation().toFile(), true);
                            PrintWriter printWriter = new PrintWriter(writer)) {
                printWriter.println(message);
            }
        } catch (IOException | CoreException e) {
            LOGGER.logError("MakefileBuilder: Failed to write to log file", e);
        }
    }
}
