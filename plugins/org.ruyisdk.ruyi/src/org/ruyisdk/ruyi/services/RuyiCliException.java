package org.ruyisdk.ruyi.services;

import java.io.IOException;

/**
 * Exception thrown when a Ruyi CLI operation fails.
 */
public class RuyiCliException extends Exception {
    private static final long serialVersionUID = 1L;

    private RuyiCliException(String message) {
        super(message);
    }

    private RuyiCliException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Ruyi executable not found. */
    public static RuyiCliException ruyiNotFound() {
        return new RuyiCliException("ruyi executable not found in configured or default install path");
    }

    /** CLI command returned non-zero exit code. CLI output is included in the message. */
    public static RuyiCliException executionFailed(String output, String commandHint) {
        final var sb = new StringBuilder("ruyi command execution failed: ").append(commandHint);
        if (output != null && !output.isBlank()) {
            sb.append("\n\nCLI Output:\n").append(output);
        }
        return new RuyiCliException(sb.toString());
    }

    /** Invalid argument provided to CLI. */
    public static RuyiCliException invalidArgument(String message) {
        return new RuyiCliException(message);
    }

    /** Operation timed out. */
    public static RuyiCliException timeout(int timeoutSeconds) {
        return new RuyiCliException("ruyi command timed out after " + timeoutSeconds + " second(s)");
    }

    /** Operation was cancelled. */
    public static RuyiCliException cancelled() {
        return new RuyiCliException("ruyi command was cancelled");
    }

    /** I/O error during CLI execution. */
    public static RuyiCliException ioError(IOException cause) {
        return new RuyiCliException("I/O error during ruyi command execution", cause);
    }
}
