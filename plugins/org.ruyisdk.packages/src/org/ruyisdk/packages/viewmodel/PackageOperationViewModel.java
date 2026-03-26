package org.ruyisdk.packages.viewmodel;

import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.packages.model.PackageOperation;
import org.ruyisdk.packages.service.PackageOperationRunner;

/**
 * ViewModel for the package-operation dialog.
 *
 * <p>
 * Manages a background {@link Job} that runs install/uninstall operations sequentially, appending
 * output text and updating the running state through property-change events. All events fire on the
 * SWT UI thread.
 */
public class PackageOperationViewModel extends BaseViewModel {

    /** Fired whenever new output text is appended. The new value is the appended text fragment. */
    public static final String PROP_OUTPUT = "output";

    /** Fired when the running state changes. The new value is a {@link Boolean}. */
    public static final String PROP_RUNNING = "running";

    private final List<PackageOperation> operations;
    private final PackageOperationRunner runner = new PackageOperationRunner();
    private final Runnable onCompleted;

    private final StringBuilder outputBuffer = new StringBuilder();
    private boolean running;
    private volatile boolean cancelled;
    private Job job;

    /**
     * Creates a new package-operation ViewModel.
     *
     * @param uiExecutor posts a {@link Runnable} to the UI thread
     * @param operations the operations to execute
     * @param onCompleted called on the UI thread after all operations finish (may be {@code null})
     */
    public PackageOperationViewModel(Consumer<Runnable> uiExecutor, List<PackageOperation> operations,
                    Runnable onCompleted) {
        super(uiExecutor);
        this.operations = List.copyOf(operations);
        this.onCompleted = onCompleted;
    }

    public boolean isRunning() {
        return running;
    }

    public String getOutput() {
        return outputBuffer.toString();
    }

    /** Start executing the queued operations in a background job. */
    public void start() {
        running = true;
        firePropertyChange(PROP_RUNNING, false, true);
        appendOutput("Starting operations...\n\n");

        job = Job.create("Package Operations", monitor -> {
            runner.run(operations, new PackageOperationRunner.OperationCallback() {
                @Override
                public void onStepStart(int index, int total, PackageOperation op) {
                    final var action = op.uninstall() ? "Uninstalling" : "Installing";
                    final var text = "[" + (index + 1) + "/" + total + "] " + action + " \"" + op.packageRef()
                                    + "\"...\n";
                    uiExecutor.accept(() -> appendOutput(text));
                }

                @Override
                public void onOutputLine(String line) {
                    uiExecutor.accept(() -> appendOutput(line + "\n"));
                }

                @Override
                public void onStepDone(int index) {
                    uiExecutor.accept(() -> appendOutput("Done.\n\n"));
                }

                @Override
                public void onStepFailed(int index, String errorMessage) {
                    uiExecutor.accept(() -> appendOutput("Failed: " + errorMessage + "\n\n"));
                }

                @Override
                public void onAllFinished(boolean wasCancelled) {
                    uiExecutor.accept(() -> {
                        if (wasCancelled) {
                            appendOutput("\nCancelled by user.\n");
                        } else {
                            appendOutput("\nAll operations completed.\n");
                        }
                        running = false;
                        firePropertyChange(PROP_RUNNING, true, false);
                        if (onCompleted != null) {
                            onCompleted.run();
                        }
                    });
                }
            }, () -> cancelled);
        });
        job.setSystem(true);
        job.schedule();
    }

    /** Request abort. The current operation will finish before stopping. */
    public void abort() {
        cancelled = true;
        if (job != null) {
            job.cancel();
        }
    }

    private void appendOutput(String text) {
        outputBuffer.append(text);
        firePropertyChange(PROP_OUTPUT, null, text);
    }
}
