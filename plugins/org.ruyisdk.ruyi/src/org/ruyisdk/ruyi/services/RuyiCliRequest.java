package org.ruyisdk.ruyi.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * An immutable, executable request to the Ruyi CLI.
 *
 * <p>
 * Use {@link #builder()} to configure command, arguments, environment, and other options, then call
 * {@link #execute()} to run the command and obtain a {@link RuyiExecResult}.
 */
public class RuyiCliRequest {

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

    /** Creates a new {@link Builder} for constructing a request. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Executes this request against the Ruyi CLI.
     *
     * @return the execution result
     * @throws RuyiCliException if the command fails, times out, or is cancelled
     */
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

        /** Sets the directory where the ruyi binary is installed. */
        public Builder ruyiInstallDir(String value) {
            this.ruyiInstallDir = value;
            return this;
        }

        /** Enables or disables porcelain (machine-readable) output. */
        public Builder porcelain(boolean value) {
            this.porcelain = value;
            return this;
        }

        /** Sets the ruyi subcommand to execute. */
        public Builder command(String value) {
            this.command = value;
            return this;
        }

        /** Appends additional command-line arguments. */
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

        /** Appends additional command-line arguments. */
        public Builder args(String... value) {
            return args(value == null ? null : Arrays.asList(value));
        }

        /** Sets environment variables to pass to the ruyi process. */
        public Builder environment(Map<String, String> value) {
            this.environment = value == null ? null : new HashMap<>(value);
            return this;
        }

        /** Sets the working directory for the ruyi process. */
        public Builder workingDirectory(File value) {
            this.workingDirectory = value;
            return this;
        }

        /** Sets a callback invoked for each line of output. */
        public Builder lineCallback(Consumer<String> value) {
            this.lineCallback = value;
            return this;
        }

        /** Sets the progress monitor for cancellation support. */
        public Builder monitor(IProgressMonitor value) {
            this.monitor = value;
            return this;
        }

        /** Sets the timeout in seconds for the ruyi process. */
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

        /** Builds an immutable {@link RuyiCliRequest} from this builder's configuration. */
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
         * Filters packages whose name contains the given string.
         *
         * @param name substring to match (empty string matches all)
         * @return this builder
         */
        public ListCommandBuilder nameContains(String name) {
            subArgs.add("--name-contains");
            subArgs.add(name);
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

        /** Sets the profile to use for the venv. */
        public VenvCommandBuilder profile(String profile) {
            this.profile = profile;
            return this;
        }

        /** Sets the destination directory for the venv. */
        public VenvCommandBuilder dest(String dest) {
            this.dest = dest;
            return this;
        }

        /** Sets the display name for the venv. */
        public VenvCommandBuilder name(String name) {
            this.name = name;
            return this;
        }

        /** Sets the toolchain package to use. */
        public VenvCommandBuilder toolchain(String toolchain) {
            this.toolchain = toolchain;
            return this;
        }

        /** Sets the emulator package to use. */
        public VenvCommandBuilder emulator(String emulator) {
            this.emulator = emulator;
            return this;
        }

        /** Enables or disables inclusion of a sysroot. */
        public VenvCommandBuilder withSysroot(boolean withSysroot) {
            this.withSysroot = withSysroot;
            return this;
        }

        /** Sets the package from which to obtain the sysroot. */
        public VenvCommandBuilder sysrootFrom(String sysrootFrom) {
            this.sysrootFrom = sysrootFrom;
            return this;
        }

        /** Sets the package from which to obtain extra commands. */
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
    }
}
