package org.ruyisdk.venv.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;

/** Service for applying venv configuration to Eclipse CDT projects. */
public class VenvConfigurationService {

    private static final String PLUGIN_ID = "org.ruyisdk.venv";
    private static final PluginLogger LOGGER = new PluginLogger(
                    Platform.getLog(FrameworkUtil.getBundle(VenvConfigurationService.class)), PLUGIN_ID);
    private static final String ENV_RUYI_VENV = "RUYI_VENV";

    /** Result of applying venv configuration to a project. */
    public static class ApplyResult {
        private final boolean success;
        private final String message;

        /** Creates a successful result. */
        public ApplyResult() {
            this.success = true;
            this.message = "Configuration applied successfully";
        }

        /** Creates a result with the given success status and message. */
        public ApplyResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        /** Returns whether the operation succeeded. */
        public boolean isSuccess() {
            return success;
        }

        /** Returns the result message. */
        public String getMessage() {
            return message;
        }
    }

    /** Applies the venv configuration to the CDT project associated with the venv. */
    private ApplyResult applyToProject(Venv venv) {
        if (venv == null) {
            LOGGER.logWarning("applyToProject called with null venv", null);
            return new ApplyResult(false, "Venv is null");
        }

        LOGGER.logInfo("Applying venv configuration: venv=" + venv.getPath() + ", project=" + venv.getProjectPath());

        final var projectPath = venv.getProjectPath();
        if (projectPath == null || projectPath.isBlank()) {
            return new ApplyResult(false, "Venv has no associated project path");
        }

        final var project = findProjectByPath(projectPath);
        if (project == null) {
            return new ApplyResult(false, "Could not find project at: " + projectPath);
        }

        if (!project.isOpen()) {
            return new ApplyResult(false, "Project is not open: " + project.getName());
        }

        try {
            // Get CDT project description
            final var coreModel = CCorePlugin.getDefault().getCoreModel();
            final var projectDesc = coreModel.getProjectDescription(project, true);

            if (projectDesc == null) {
                return new ApplyResult(false, "Project has no CDT configuration: " + project.getName());
            }

            // Configure all build configurations
            var anyConfigured = false;
            for (final var configDesc : projectDesc.getConfigurations()) {
                if (configDesc == null) {
                    continue;
                }

                // Configure environment variables for this configuration
                configureEnvironmentVariables(configDesc, venv);

                // Configure Cross GCC toolchain settings
                configureCrossGccToolchain(project, configDesc, venv);

                anyConfigured = true;
            }

            if (!anyConfigured) {
                return new ApplyResult(false, "No build configurations found in project");
            }

            // Save the project description
            coreModel.setProjectDescription(project, projectDesc);

            // Build detailed success message
            final var sb = new StringBuilder();
            sb.append("CDT configuration applied to project: ").append(project.getName());
            sb.append("\n\nConfigured:");
            sb.append("\n- RUYI_VENV: ").append(venv.getPath());
            final var toolchainPath = venv.getToolchainPath();
            if (toolchainPath != null && !toolchainPath.isEmpty()) {
                sb.append("\n- PATH prepended with: ").append(toolchainPath);
            }
            final var toolchainPrefix = venv.getToolchainPrefix();
            if (toolchainPrefix != null && !toolchainPrefix.isEmpty()) {
                sb.append("\n- Toolchain Prefix: ").append(toolchainPrefix);
            }

            LOGGER.logInfo("Venv configuration applied successfully: project=" + project.getName());
            return new ApplyResult(true, sb.toString());

        } catch (CoreException e) {
            LOGGER.logError("Failed to apply CDT configuration: project=" + projectPath, e);
            return new ApplyResult(false, "Failed to apply CDT configuration: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.logError("Unexpected error applying venv configuration: project=" + projectPath, e);
            return new ApplyResult(false, "Unexpected error: " + e.getMessage());
        }
    }

    /** Configures environment variables for a CDT build configuration. */
    private void configureEnvironmentVariables(ICConfigurationDescription configDesc, Venv venv) {
        final var envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        final var contribEnv = envManager.getContributedEnvironment();

        // Set RUYI_VENV
        final var venvPath = venv.getPath();
        if (venvPath != null && !venvPath.isEmpty()) {
            contribEnv.addVariable(ENV_RUYI_VENV, venvPath, IEnvironmentVariable.ENVVAR_REPLACE, null, configDesc);
        }

        // Prepend toolchain bin directory to PATH so build tools are found
        final var toolchainPath = venv.getToolchainPath();
        if (toolchainPath != null && !toolchainPath.isEmpty()) {
            contribEnv.addVariable("PATH", toolchainPath, IEnvironmentVariable.ENVVAR_PREPEND,
                            System.getProperty("path.separator"), configDesc);
        }
    }

    /** Configures Cross GCC toolchain prefix settings. */
    private void configureCrossGccToolchain(IProject project, ICConfigurationDescription configDesc, Venv venv) {
        final var buildInfo = ManagedBuildManager.getBuildInfo(project);
        if (buildInfo == null) {
            return;
        }

        var config = buildInfo.getManagedProject().getConfiguration(configDesc.getId());
        if (config == null) {
            // Try to get configuration by name
            for (final var cfg : buildInfo.getManagedProject().getConfigurations()) {
                if (cfg != null && cfg.getName().equals(configDesc.getName())) {
                    config = cfg;
                    break;
                }
            }
        }

        if (config == null) {
            return;
        }

        final var toolChain = config.getToolChain();
        if (toolChain == null) {
            return;
        }

        // Set the command prefix option on the toolchain
        // Eclipse Embedded CDT uses this to build the full command (prefix + gcc, prefix + g++, etc.)
        final var toolchainPrefix = venv.getToolchainPrefix();
        if (toolchainPrefix != null && !toolchainPrefix.isEmpty()) {
            // Cross GCC uses prefix with trailing dash
            final var prefixWithDash = toolchainPrefix.endsWith("-") ? toolchainPrefix : toolchainPrefix + "-";

            // Set Eclipse Embedded CDT RISC-V toolchain prefix
            setToolChainOptionBySuperClassId(config, toolChain,
                            "ilg.gnumcueclipse.managedbuild.cross.riscv.option.command.prefix", prefixWithDash);
        }

        // Save managed build info changes
        ManagedBuildManager.saveBuildInfo(project, true);
    }

    /** Sets a toolchain option by super class ID if it exists. */
    private void setToolChainOptionBySuperClassId(IConfiguration config, IToolChain toolChain, String optionId,
                    String value) {
        try {
            final var option = toolChain.getOptionBySuperClassId(optionId);
            if (option != null) {
                config.setOption(toolChain, option, value);
            }
        } catch (Exception e) {
            // Option doesn't exist or can't be set - ignore
        }
    }

    /** Applies the venv configuration to the project asynchronously. */
    public void applyToProjectAsync(Venv venv, Consumer<ApplyResult> callback) {
        final var applyJob = new Job("Applying venv configuration to CDT project") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final var result = applyToProject(venv);
                if (callback != null) {
                    callback.accept(result);
                }
                return result.isSuccess() ? Status.OK_STATUS
                                : new Status(IStatus.WARNING, PLUGIN_ID, result.getMessage());
            }
        };
        applyJob.schedule();
    }

    /** Finds a project by its file system path, using normalized path comparison. */
    private IProject findProjectByPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            return null;
        }

        final var normalizedTarget = normalizePath(projectPath);
        if (normalizedTarget == null) {
            return null;
        }

        final var root = ResourcesPlugin.getWorkspace().getRoot();
        for (final var project : root.getProjects()) {
            if (project == null) {
                continue;
            }
            final var location = project.getLocation();
            if (location == null) {
                continue;
            }
            final var projectLocation = normalizePath(location.toOSString());
            if (projectLocation != null && projectLocation.equals(normalizedTarget)) {
                return project;
            }
        }
        return null;
    }

    /** Normalizes a file system path, resolving symlinks if possible. */
    private Path normalizePath(String pathString) {
        if (pathString == null || pathString.isBlank()) {
            return null;
        }
        try {
            return Paths.get(pathString).toRealPath();
        } catch (Exception e) {
            // Fall back to normalized path if real path resolution fails (e.g., path doesn't exist)
            try {
                return Paths.get(pathString).normalize().toAbsolutePath();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
