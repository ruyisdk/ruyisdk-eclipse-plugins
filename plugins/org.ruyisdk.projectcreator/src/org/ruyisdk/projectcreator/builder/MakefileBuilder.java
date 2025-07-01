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
import org.ruyisdk.projectcreator.Activator;

public class MakefileBuilder extends IncrementalProjectBuilder {

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
		File projectLocation = project.getLocation().toFile();

		// Create obj directory if it doesn't exist
		IFolder objFolder = project.getFolder(OBJ_DIR_NAME);
		if (!objFolder.exists()) {
			objFolder.create(true, true, monitor);
		}

		clearLogFile(project);
		logToFile(project, "=== Build started at " + new Date() + " ===");

		// 1. Ensure Ruyi virtual environment exists
		try {
			ensureRuyiVenv(project, monitor);
		} catch (IOException | InterruptedException e) {
			String errorMsg = "Failed to create Ruyi virtual environment: " + e.getMessage();
			logToFile(project, errorMsg);
			e.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMsg, e));
		}

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
			String errorMsg = "Could not find RISC-V GCC in Ruyi virtual environment at: "
					+ venvBinDir.getAbsolutePath();
			logToFile(project, errorMsg);
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMsg));
		}

		logToFile(project, "Found RISC-V GCC at: " + gccPath);

		// Extract the prefix from the GCC path
		String gccName = new File(gccPath).getName();
		String prefix = gccName.replace("-gcc", "");
		logToFile(project, "Using toolchain prefix: " + prefix);

		// Determine the correct CFLAGS based on the toolchain
		String cflags = determineCorrectCFLAGS(gccPath, project);
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
					System.out.println(">>> MakefileBuilder: " + line);
					logToFile(project, line);
				}
			}
			int exitCode = process.waitFor();
			String resultMessage = "Build finished with exit code: " + exitCode;
			logToFile(project, resultMessage);

		} catch (Exception e) {
			String errorMessage = "Build failed with exception: " + e.getMessage();
			System.err.println(">>> MakefileBuilder: " + errorMessage);
			e.printStackTrace();
			logToFile(project, errorMessage);
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
	 * @throws CoreException if board model is not set
	 */
	private String determineCorrectCFLAGS(String gccPath, IProject project) throws CoreException {
		// Base CFLAGS that are always included
		String cflags = "-Wall -O2";

		// Get the board model to provide more specific flags
		String boardModel = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BOARD_MODEL_PROPERTY));
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

	private String getRuyiInstallPath() {
		try {
			Class<?> clazz = Class.forName("org.ruyisdk.ruyi.util.RuyiFileUtils");
			java.lang.reflect.Method method = clazz.getMethod("getInstallPath");
			return (String) method.invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
			return System.getProperty("user.home") + "/.ruyi";
		}
	}

	private void ensureRuyiVenv(IProject project, IProgressMonitor monitor)
			throws IOException, InterruptedException, CoreException {
		File projectLocation = project.getLocation().toFile();
		File venvDir = new File(projectLocation, RUYI_VENV_DIR);

		if (venvDir.exists() && venvDir.isDirectory()) {
			logToFile(project, "Ruyi virtual environment found at: " + venvDir.getAbsolutePath());
			return; // Venv already exists, do nothing.
		}

		logToFile(project, "Ruyi virtual environment not found. Creating...");

		String venvCommand = project
				.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, RUYI_VENV_CMD_PROPERTY));

		if (venvCommand == null || venvCommand.trim().isEmpty()) {

			logToFile(project, "No custom venv command found, generating default command...");
			String boardModel = project
					.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BOARD_MODEL_PROPERTY));
			if (boardModel == null) {
				throw new CoreException(
						new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Board model not set for project."));
			}
			String toolchain = org.ruyisdk.packages.JsonParser.findInstalledToolchainForBoard(boardModel);
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
			String ruyiPath = getRuyiInstallPath() + "/ruyi";

			venvCommand = String.format("%s venv -t %s %s ./%s", ruyiPath, toolchain, boardModel, RUYI_VENV_DIR);
		} else {
			logToFile(project, "Using custom ruyi venv command from project properties.");
		}

		ProcessBuilder pb = new ProcessBuilder("bash", "-c", venvCommand);
		System.err.println("Executing: " + venvCommand);
		pb.directory(projectLocation);
		pb.redirectErrorStream(true);

		logToFile(project, "Executing: " + venvCommand);
		Process process = pb.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				logToFile(project, "[ruyi venv] " + line);
			}
		}

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to create Ruyi virtual environment. Exit code: " + exitCode));
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
			System.err.println(">>> MakefileBuilder: Failed to clear log file: " + e.getMessage());
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
			System.err.println(">>> MakefileBuilder: Failed to write to log file: " + e.getMessage());
		}
	}
}