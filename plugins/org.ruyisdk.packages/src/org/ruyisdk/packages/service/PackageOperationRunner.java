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
                final Consumer<String> lineCallback = callback::onOutputLine;
                if (op.uninstall()) {
                    RuyiCli.uninstallPackageStreaming(op.packageRef(), true, lineCallback, null);
                } else {
                    RuyiCli.installPackageStreaming(op.packageRef(), lineCallback, null);
                }
                callback.onStepDone(i);
            } catch (Exception e) {
                callback.onStepFailed(i, e.getMessage());
            }
        }
        callback.onAllFinished(cancelFlag.getAsBoolean());
    }
}
