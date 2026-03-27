package org.ruyisdk.packages.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.ruyisdk.packages.model.PackageOperation;

/**
 * Unit tests for {@link PackageOperationRunner} covering streaming output sequencing, failure
 * behaviour, and cancellation.
 */
public class PackageOperationRunnerTest {

    // ------------------------------------------------------------------
    // Multiple successful operations
    // ------------------------------------------------------------------

    @Test
    public void multipleSuccessfulOperations_streamsOutputAndCompletes() {
        List<String> events = new ArrayList<>();

        // Installer stub that emits lines and succeeds
        PackageOperationRunner.PackageInstaller installer = (op, lineCallback) -> {
            lineCallback.accept(op.packageRef() + "-line1");
            lineCallback.accept(op.packageRef() + "-line2");
        };

        PackageOperationRunner runner = new PackageOperationRunner(installer);

        List<PackageOperation> ops = List.of(
                        new PackageOperation("pkg-a(1.0)", false),
                        new PackageOperation("pkg-b(2.0)", false));

        runner.run(ops, new RecordingCallback(events), () -> false);

        assertEquals("onStepStart:0:2:pkg-a(1.0)", events.get(0));
        assertEquals("onOutputLine:pkg-a(1.0)-line1", events.get(1));
        assertEquals("onOutputLine:pkg-a(1.0)-line2", events.get(2));
        assertEquals("onStepDone:0", events.get(3));
        assertEquals("onStepStart:1:2:pkg-b(2.0)", events.get(4));
        assertEquals("onOutputLine:pkg-b(2.0)-line1", events.get(5));
        assertEquals("onOutputLine:pkg-b(2.0)-line2", events.get(6));
        assertEquals("onStepDone:1", events.get(7));
        assertEquals("onAllFinished:false", events.get(events.size() - 1));
    }

    // ------------------------------------------------------------------
    // Failing operation
    // ------------------------------------------------------------------

    @Test
    public void failingOperation_triggersOnStepFailedAndContinues() {
        List<String> events = new ArrayList<>();

        PackageOperationRunner.PackageInstaller installer = (op, lineCallback) -> {
            lineCallback.accept("output");
            if ("bad(1.0)".equals(op.packageRef())) {
                throw new RuntimeException("install failed");
            }
        };

        PackageOperationRunner runner = new PackageOperationRunner(installer);

        List<PackageOperation> ops = List.of(
                        new PackageOperation("good(1.0)", false),
                        new PackageOperation("bad(1.0)", false),
                        new PackageOperation("after(1.0)", false));

        runner.run(ops, new RecordingCallback(events), () -> false);

        // First step succeeds
        assertTrue(events.contains("onStepDone:0"));

        // Second step fails
        assertTrue(events.contains("onStepFailed:1:install failed"));

        // Third step still runs (runner does not abort on failure)
        assertTrue(events.contains("onStepStart:2:3:after(1.0)"));
        assertTrue(events.contains("onStepDone:2"));

        assertEquals("onAllFinished:false", events.get(events.size() - 1));
    }

    // ------------------------------------------------------------------
    // Cancellation between steps
    // ------------------------------------------------------------------

    @Test
    public void cancelledBeforeNextStep_stopsLoopAndReportsCancelled() {
        List<String> events = new ArrayList<>();
        AtomicBoolean cancelFlag = new AtomicBoolean(false);

        PackageOperationRunner.PackageInstaller installer = (op, lineCallback) -> {
            lineCallback.accept("output");
        };

        PackageOperationRunner runner = new PackageOperationRunner(installer);

        List<PackageOperation> ops = List.of(
                        new PackageOperation("step1(1.0)", false),
                        new PackageOperation("step2(1.0)", false));

        // Cancel after first step completes
        RecordingCallback callback = new RecordingCallback(events) {
            @Override
            public void onStepDone(int index) {
                super.onStepDone(index);
                if (index == 0) {
                    cancelFlag.set(true);
                }
            }
        };

        runner.run(ops, callback, cancelFlag::get);

        // First step should complete
        assertTrue(events.contains("onStepStart:0:2:step1(1.0)"));
        assertTrue(events.contains("onStepDone:0"));

        // Second step should not run
        for (String e : events) {
            assertFalse("step2 should not appear: " + e, e.contains("step2"));
        }

        // Final callback should indicate cancelled
        assertEquals("onAllFinished:true", events.get(events.size() - 1));
    }

    // ------------------------------------------------------------------
    // Uninstall operation dispatches correctly
    // ------------------------------------------------------------------

    @Test
    public void uninstallOperation_passedToInstaller() {
        List<String> events = new ArrayList<>();
        List<PackageOperation> receivedOps = new ArrayList<>();

        PackageOperationRunner.PackageInstaller installer = (op, lineCallback) -> {
            receivedOps.add(op);
        };

        PackageOperationRunner runner = new PackageOperationRunner(installer);

        List<PackageOperation> ops = List.of(
                        new PackageOperation("pkg(1.0)", true));

        runner.run(ops, new RecordingCallback(events), () -> false);

        assertEquals(1, receivedOps.size());
        assertTrue(receivedOps.get(0).uninstall());
        assertEquals("onAllFinished:false", events.get(events.size() - 1));
    }

    // ------------------------------------------------------------------
    // Test helpers
    // ------------------------------------------------------------------

    private static class RecordingCallback implements PackageOperationRunner.OperationCallback {

        private final List<String> events;

        RecordingCallback(List<String> events) {
            this.events = events;
        }

        @Override
        public void onStepStart(int index, int total, PackageOperation operation) {
            events.add("onStepStart:" + index + ":" + total + ":" + operation.packageRef());
        }

        @Override
        public void onOutputLine(String line) {
            events.add("onOutputLine:" + line);
        }

        @Override
        public void onStepDone(int index) {
            events.add("onStepDone:" + index);
        }

        @Override
        public void onStepFailed(int index, String errorMessage) {
            events.add("onStepFailed:" + index + ":" + errorMessage);
        }

        @Override
        public void onAllFinished(boolean wasCancelled) {
            events.add("onAllFinished:" + wasCancelled);
        }
    }
}
