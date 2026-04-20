package org.ruyisdk.ruyi.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;

/**
 * Shared executor for invoking ruyi commands with timeout support.
 *
 * <p>
 * All process lifecycle management is handled internally. No {@link Process} objects are exposed to
 * callers. Higher-level request assembly is handled elsewhere; this class only launches and manages
 * the process.
 */
public final class RuyiCliExecutor {

    private static final PluginLogger LOGGER = Activator.getLogger();
    private static final long WAIT_SLICE_MILLIS = 100L;
    private static final long OUTPUT_JOIN_TIMEOUT_MILLIS = 500L;

    private RuyiCliExecutor() {}

    /**
     * Executes a ruyi command with a working directory, optional real-time line output and
     * cancellation support.
     *
     * @param ruyiInstallDir ruyi installation directory
     * @param environment extra environment entries (may be {@code null})
     * @param workingDirectory working directory for the process (may be {@code null})
     * @param lineCallback called for each output line (may be {@code null})
     * @param monitor progress monitor for cancellation (may be {@code null})
     * @param timeoutSeconds maximum seconds to wait, 0 for unlimited
     * @param args ruyi arguments
     * @return command result with exit code and accumulated output
     */
    public static RuyiExecResult execute(String ruyiInstallDir, Map<String, String> environment,
            File workingDirectory, Consumer<String> lineCallback, IProgressMonitor monitor,
            int timeoutSeconds, String... args) {
        final var command = buildCommand(ruyiInstallDir, args);
        return executeCommand(command, environment, workingDirectory, true, lineCallback, monitor,
                timeoutSeconds);
    }

    private static List<String> buildCommand(String ruyiInstallDir, String... args) {
        final var command = new ArrayList<String>();
        command.add(getRuyiBinaryPath(ruyiInstallDir));
        for (String arg : args) {
            command.add(arg);
        }
        return command;
    }

    private static String getRuyiBinaryPath(String ruyiInstallDir) {
        return Paths.get(ruyiInstallDir, "ruyi").toString();
    }

    private static Process startProcess(List<String> command, Map<String, String> environment,
            File workingDirectory, boolean redirectErrorStream) {
        final var processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(redirectErrorStream);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory);
        }
        if (environment != null && !environment.isEmpty()) {
            processBuilder.environment().putAll(environment);
        }
        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw RuyiCliException.ioError(e);
        }
    }

    private static RuyiExecResult executeCommand(List<String> command,
            Map<String, String> environment, File workingDirectory, boolean redirectErrorStream,
            Consumer<String> lineCallback, IProgressMonitor monitor, int timeoutSeconds) {
        LOGGER.logInfo(String.format("[RuyiCliExecutor] Executing ruyi command: [%s]",
                String.join("] [", command)));
        final var process =
                startProcess(command, environment, workingDirectory, redirectErrorStream);
        final var outputFuture = CompletableFuture.supplyAsync(() -> {
            return readLines(process, lineCallback, monitor);
        });
        final long deadlineNanos =
                timeoutSeconds > 0 ? System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
                        : Long.MAX_VALUE;
        try {
            waitForProcess(process, monitor, deadlineNanos, timeoutSeconds);
            final var output = awaitOutput(outputFuture, deadlineNanos, timeoutSeconds);
            final var exitCode = process.exitValue();
            LOGGER.logInfo("[RuyiCliExecutor] ruyi command exited with code: " + exitCode);
            return new RuyiExecResult(exitCode, output);
        } finally {
            cleanupProcess(process, outputFuture);
        }
    }

    private static void cleanupProcess(Process process, CompletableFuture<String> outputFuture) {
        outputFuture.cancel(true);

        try {
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException e) {
            // ignore
        }

        try {
            process.destroy();
            if (!process.waitFor(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS) && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore
        }

        try {
            outputFuture.get(OUTPUT_JOIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException
                | CancellationException e) {
            // why not "throws CancellationException" but documents it???
            // ignore
        }
    }

    private static void waitForProcess(Process process, IProgressMonitor monitor,
            long deadlineNanos, int timeoutSeconds) {
        while (true) {
            if (monitor != null && monitor.isCanceled()) {
                throw RuyiCliException.cancelled();
            }
            try {
                if (process.waitFor(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw RuyiCliException.cancelled();
            }
            if (timeoutSeconds > 0 && System.nanoTime() >= deadlineNanos) {
                throw RuyiCliException.timeout(timeoutSeconds);
            }
        }
    }

    private static String awaitOutput(CompletableFuture<String> outputFuture, long deadlineNanos,
            int timeoutSeconds) {
        try {
            if (timeoutSeconds <= 0) {
                return outputFuture.get();
            }
            final long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                throw RuyiCliException.timeout(timeoutSeconds);
            }
            return outputFuture.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            LOGGER.logWarning(String.format("[RuyiCliExecutor] ruyi command timed out after %d s",
                    timeoutSeconds));
            throw RuyiCliException.timeout(timeoutSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RuyiCliException.cancelled();
        } catch (ExecutionException e) {
            throw RuyiCliException.executionError(e);
        }
    }

    /**
     * Reads process output line-by-line using {@link Process#inputReader()}. Supports optional
     * per-line callback and cancellation via monitor.
     */
    private static String readLines(Process process, Consumer<String> lineCallback,
            IProgressMonitor monitor) {
        final var output = new StringBuilder();
        try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (monitor != null && monitor.isCanceled()) {
                    throw RuyiCliException.cancelled();
                }
                output.append(line).append(System.lineSeparator());

                if (lineCallback != null) {
                    lineCallback.accept(line);
                }
            }
            if (monitor != null && monitor.isCanceled()) {
                throw RuyiCliException.cancelled();
            }

            LOGGER.logInfo("[RuyiCliExecutor] Finished reading output");
            return output.toString();
        } catch (IOException e) {
            throw RuyiCliException.ioError(e);
        }
    }
}
