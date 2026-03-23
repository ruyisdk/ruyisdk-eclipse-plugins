package org.ruyisdk.ruyi.services;

/**
 * Result of executing an external command.
 */
public class RuyiExecResult {
    private final int exitCode;
    private final String output;

    /**
     * Creates a command execution result.
     *
     * @param exitCode process exit code
     * @param output captured process output
     */
    public RuyiExecResult(int exitCode, String output) {
        this.exitCode = exitCode;
        this.output = output;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }
}
