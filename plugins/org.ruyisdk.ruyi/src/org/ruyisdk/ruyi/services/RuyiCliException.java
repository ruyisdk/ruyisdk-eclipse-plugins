package org.ruyisdk.ruyi.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.ruyisdk.core.exception.PluginException;

/**
 * Exception thrown when a Ruyi CLI operation fails.
 */
public class RuyiCliException extends PluginException {
    private static final long serialVersionUID = 1L;

    private RuyiCliException(String message) {
        super(message);
    }

    private RuyiCliException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Ruyi executable not found. */
    public static RuyiCliException ruyiNotFound() {
        return new RuyiCliException(
                "ruyi executable not found in configured or default install path");
    }

    /** CLI command returned non-zero exit code. CLI output is included in the message. */
    public static RuyiCliException executionFailed(String commandHint, int exitCode,
            String output) {
        final var message = String.format("""
            ruyi command execution failed with code: %d
            Command: %s
            CLI Output:
            %s""", exitCode, commandHint, output);
        return new RuyiCliException(message);
    }

    /** Invalid argument provided to CLI. */
    public static RuyiCliException invalidArgument(String message) {
        return new RuyiCliException(message);
    }

    /** Operation timed out. */
    public static RuyiCliException timeout(int timeoutSeconds) {
        return new RuyiCliException(
                "ruyi command timed out after " + timeoutSeconds + " second(s)");
    }

    /** Operation was cancelled. */
    public static RuyiCliException cancelled() {
        return new RuyiCliException("ruyi command was cancelled");
    }

    /** I/O error during CLI execution. */
    public static RuyiCliException ioError(IOException cause) {
        return new RuyiCliException("I/O error during ruyi command execution", cause);
    }

    /** {@link ExecutionException} during CLI execution. */
    public static RuyiCliException executionError(ExecutionException e) {
        return new RuyiCliException("Unexpected error during ruyi command execution", e.getCause());
    }

    /** Eclipse terminal service is not available. */
    public static RuyiCliException terminalUnavailable() {
        return new RuyiCliException("Eclipse terminal service is unavailable");
    }

    /** Installed ruyi version is unsupported or cannot be detected. */
    public static RuyiCliException unsupportedVersion(String minimumVersion,
            String currentVersion) {
        if (currentVersion == null) {
            return new RuyiCliException(String.format(
                    "Unable to detect installed ruyi version. Minimum required version is %s",
                    minimumVersion));
        } else {
            return new RuyiCliException(String.format(
                    "Installed ruyi version %s is unsupported. Minimum required version is %s",
                    currentVersion, minimumVersion));
        }
    }
}
