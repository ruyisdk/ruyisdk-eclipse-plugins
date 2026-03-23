package org.ruyisdk.ruyi.services;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
     * Executes a ruyi command with a working directory, optional real-time line output and cancellation
     * support.
     *
     * @param ruyiInstallDir ruyi installation directory
     * @param environment extra environment entries (may be {@code null})
     * @param workingDirectory working directory for the process (may be {@code null})
     * @param lineCallback called for each output line (may be {@code null})
     * @param monitor progress monitor for cancellation (may be {@code null})
     * @param timeoutSeconds maximum seconds to wait, 0 for unlimited
     * @param args ruyi arguments
     * @return command result with exit code and accumulated output
     * @throws IOException if process launch fails
     * @throws InterruptedException if interrupted while waiting
     * @throws OperationCanceledException if the monitor is cancelled
     */
    public static RuyiExecResult execute(String ruyiInstallDir, Map<String, String> environment, File workingDirectory,
                    Consumer<String> lineCallback, IProgressMonitor monitor, int timeoutSeconds, String... args)
                    throws IOException, InterruptedException {
        final var command = buildCommand(ruyiInstallDir, args);
        return executeCommand(command, environment, workingDirectory, true, lineCallback, monitor, timeoutSeconds);
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

    private static Process startProcess(List<String> command, Map<String, String> environment, File workingDirectory,
                    boolean redirectErrorStream) throws IOException {
        final var processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(redirectErrorStream);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory);
        }
        if (environment != null && !environment.isEmpty()) {
            processBuilder.environment().putAll(environment);
        }
        return processBuilder.start();
    }

    private static RuyiExecResult executeCommand(List<String> command, Map<String, String> environment,
                    File workingDirectory, boolean redirectErrorStream, Consumer<String> lineCallback,
                    IProgressMonitor monitor, int timeoutSeconds) throws IOException, InterruptedException {
        LOGGER.logInfo("[RuyiCliExecutor] Executing ruyi command: [" + String.join("] [", command) + "]");
        final var process = startProcess(command, environment, workingDirectory, redirectErrorStream);
        final var outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readLines(process, lineCallback, monitor);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
        final long deadlineNanos = timeoutSeconds > 0 ? System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
                        : Long.MAX_VALUE;
        var cleanupStreams = true;
        var cancelOutputFuture = false;
        try {
            waitForProcess(process, monitor, deadlineNanos, timeoutSeconds);
            final var output = awaitOutput(outputFuture, deadlineNanos, timeoutSeconds);
            final var exitCode = process.exitValue();
            LOGGER.logInfo("[RuyiCliExecutor] ruyi command exited with code: " + exitCode);
            return new RuyiExecResult(exitCode, output);
        } catch (OperationCanceledException e) {
            cancelOutputFuture = true;
            LOGGER.logInfo("[RuyiCliExecutor] ruyi command cancelled");
            throw e;
        } catch (TimeoutException e) {
            cancelOutputFuture = true;
            LOGGER.logInfo("[RuyiCliExecutor] ruyi command timed out after " + timeoutSeconds + "s");
            return new RuyiExecResult(-1, "ruyi command timed out");
        } catch (InterruptedException e) {
            cancelOutputFuture = true;
            throw e;
        } catch (ExecutionException e) {
            cleanupStreams = false;

            final var cause = e.getCause();
            if (cause instanceof CompletionException && cause.getCause() != null) {
                throw unwrapThrowable(cause.getCause());
            }
            throw unwrapThrowable(cause);
        } finally {
            cleanupProcess(process, outputFuture, cancelOutputFuture, cleanupStreams);
        }
    }

    private static void cleanupProcess(Process process, CompletableFuture<String> outputFuture,
                    boolean cancelOutputFuture, boolean closeStreams) throws InterruptedException {
        if (cancelOutputFuture) {
            outputFuture.cancel(true);
        }
        if (closeStreams) {
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            closeQuietly(process.getOutputStream());
        }

        if (process.isAlive()) {
            process.destroy();
            if (!process.waitFor(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS) && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }

        try {
            outputFuture.get(OUTPUT_JOIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (CancellationException | ExecutionException | TimeoutException e) {
            LOGGER.logInfo("[RuyiCliExecutor] Output reader finished during cleanup: " + e.getClass().getSimpleName());
        }
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            LOGGER.logInfo("[RuyiCliExecutor] Ignoring close failure during process cleanup: " + e.getMessage());
        }
    }

    private static void waitForProcess(Process process, IProgressMonitor monitor, long deadlineNanos,
                    int timeoutSeconds) throws InterruptedException, TimeoutException {
        while (true) {
            if (monitor != null && monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            if (process.waitFor(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS)) {
                return;
            }
            if (timeoutSeconds > 0 && System.nanoTime() >= deadlineNanos) {
                throw new TimeoutException();
            }
        }
    }

    private static String awaitOutput(CompletableFuture<String> outputFuture, long deadlineNanos, int timeoutSeconds)
                    throws InterruptedException, ExecutionException, TimeoutException {
        if (timeoutSeconds <= 0) {
            return outputFuture.get();
        }
        final long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new TimeoutException();
        }
        return outputFuture.get(remainingNanos, TimeUnit.NANOSECONDS);
    }

    private static IOException unwrapThrowable(Throwable throwable) throws IOException {
        if (throwable instanceof IOException ioException) {
            return ioException;
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IOException("Failed to read process output", throwable);
    }

    /**
     * Reads process output line-by-line using {@link Process#inputReader()}. Supports optional per-line
     * callback and cancellation via monitor.
     */
    private static String readLines(Process process, Consumer<String> lineCallback, IProgressMonitor monitor)
                    throws IOException {
        final var output = new StringBuilder();
        try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (monitor != null && monitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
                output.append(line).append(System.lineSeparator());

                if (lineCallback != null) {
                    lineCallback.accept(line);
                }
            }
            if (monitor != null && monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            LOGGER.logInfo("[RuyiCliExecutor] Finished reading output");
            return output.toString();
        }
    }
}
