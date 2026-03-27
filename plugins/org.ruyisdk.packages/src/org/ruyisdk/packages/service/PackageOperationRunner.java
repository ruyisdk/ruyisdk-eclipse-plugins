package org.ruyisdk.packages.service;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.ruyisdk.packages.model.PackageOperation;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * Executes a list of package install/uninstall operations synchronously, reporting progress through
 * an {@link OperationCallback}. The caller is responsible for running this off the UI thread.
 */
public class PackageOperationRunner {

    /**
     * Abstraction over CLI install/uninstall so that the runner can be tested without {@link RuyiCli}.
     */
    @FunctionalInterface
    public interface PackageInstaller {

        /**
         * Performs an install or uninstall.
         *
         * @param op the operation to execute
         * @param lineCallback called for each line of process output
         * @throws Exception if the operation fails
         */
        void execute(PackageOperation op, Consumer<String> lineCallback) throws Exception;
    }

    /** Default installer that delegates to {@link RuyiCli}. */
    public static final PackageInstaller DEFAULT_INSTALLER = (op, lineCallback) -> {
        if (op.uninstall()) {
            RuyiCli.uninstallPackageStreaming(op.packageRef(), true, lineCallback, null);
        } else {
            RuyiCli.installPackageStreaming(op.packageRef(), lineCallback, null);
        }
    };

    /**
     * Callback interface for operation progress reporting.
     */
    public interface OperationCallback {

        /** Called when a step is about to start. */
        void onStepStart(int index, int total, PackageOperation operation);

        /** Called for each line of process output. */
        void onOutputLine(String line);

        /** Called when a step completes successfully. */
        void onStepDone(int index);

        /** Called when a step fails. */
        void onStepFailed(int index, String errorMessage);

        /** Called after all operations finish (or are cancelled). */
        void onAllFinished(boolean wasCancelled);
    }

    private final PackageInstaller installer;

    /** Creates a runner using the default {@link RuyiCli}-backed installer. */
    public PackageOperationRunner() {
        this(DEFAULT_INSTALLER);
    }

    /**
     * Creates a runner with the given installer (useful for testing).
     *
     * @param installer the installer to delegate individual operations to
     */
    public PackageOperationRunner(PackageInstaller installer) {
        this.installer = installer;
    }

    /**
     * Runs all operations in order. Checks {@code cancelFlag} between operations; a running operation
     * is not interrupted.
     *
     * @param operations the operations to execute
     * @param callback progress callback
     * @param cancelFlag supplier returning {@code true} when cancellation has been requested
     */
    public void run(List<PackageOperation> operations, OperationCallback callback, BooleanSupplier cancelFlag) {
        for (int i = 0; i < operations.size(); i++) {
            if (cancelFlag.getAsBoolean()) {
                break;
            }
            final var op = operations.get(i);
            callback.onStepStart(i, operations.size(), op);

            try {
                installer.execute(op, callback::onOutputLine);
                callback.onStepDone(i);
            } catch (Exception e) {
                callback.onStepFailed(i, e.getMessage());
            }
        }
        callback.onAllFinished(cancelFlag.getAsBoolean());
    }
}
