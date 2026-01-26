package org.ruyisdk.ruyi.core.workspace;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.util.RuyiLogger;

/**
 * Monitors Eclipse workspace open-project changes and provides a debounced change signal.
 */
public final class WorkspaceProjectsMonitor {

    private static final RuyiLogger LOGGER = Activator.getLogger();

    private static final int AUTO_REFRESH_DELAY_MS = 750;

    /** Workspace projects event kind. */
    public enum EventKind {
        /** Open projects may have changed (add/remove/open/close). */
        PROJECTS_CHANGED,

        /** Debounce window elapsed after a relevant change. */
        DEBOUNCE_TRIGGERED,
    }

    /** Workspace projects event. */
    public static final class Event {
        private final EventKind kind;
        private final boolean hasOpenProjects;

        private Event(EventKind kind, boolean hasOpenProjects) {
            this.kind = kind;
            this.hasOpenProjects = hasOpenProjects;
        }

        /**
         * Returns the event kind.
         *
         * @return the event kind
         */
        public EventKind getKind() {
            return kind;
        }

        /**
         * Returns whether the workspace had any open projects at the time of the event.
         *
         * @return whether there are open projects
         */
        public boolean hasOpenProjects() {
            return hasOpenProjects;
        }
    }

    /** Listener for workspace projects events. */
    public interface Listener {
        /**
         * Called when a workspace projects event occurs.
         *
         * <p>
         * This method may be called from a non-UI thread.
         * </p>
         *
         * @param event the event
         */
        void onWorkspaceProjectsEvent(Event event);
    }

    private static final WorkspaceProjectsMonitor INSTANCE = new WorkspaceProjectsMonitor();

    /**
     * Returns the singleton instance.
     *
     * @return the workspace projects monitor
     */
    public static WorkspaceProjectsMonitor getInstance() {
        return INSTANCE;
    }

    private final List<WeakReference<Listener>> listeners = new CopyOnWriteArrayList<>();
    private final Job debounceJob = new Job("Ruyi workspace projects debounce") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            notifyListeners(EventKind.DEBOUNCE_TRIGGERED, hasOpenProjects());
            return Status.OK_STATUS;
        }
    };

    private final IResourceChangeListener resourceChangeListener = this::resourceChanged;

    private volatile boolean installed = false;

    private WorkspaceProjectsMonitor() {
        debounceJob.setSystem(true);
    }

    /**
     * Adds a listener for workspace projects events.
     *
     * <p>
     * The listener is held weakly.
     * </p>
     *
     * @param listener the listener
     */
    public void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(new WeakReference<>(listener));
        ensureInstalled();
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener
     */
    public void removeListener(Listener listener) {
        if (listener == null) {
            return;
        }
        listeners.removeIf(ref -> {
            if (ref == null) {
                return true;
            }
            final var _listener = ref.get();
            return _listener == null || _listener == listener;
        });
    }

    /**
     * Disposes the monitor by removing workspace listeners and cancelling pending debounce work.
     */
    public void dispose() {
        try {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
        } catch (Exception e) {
            // ignore
        }
        debounceJob.cancel();
        listeners.clear();
        installed = false;
    }

    /**
     * Returns whether the workspace currently has any open projects.
     *
     * @return whether there are open projects
     */
    public boolean hasOpenProjects() {
        try {
            final var root = ResourcesPlugin.getWorkspace().getRoot();
            for (final var project : root.getProjects()) {
                if (project != null && project.isOpen()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore workspace discovery failures
        }
        return false;
    }

    /**
     * Returns the root paths of currently-open projects.
     *
     * <p>
     * The result is sorted to provide stable UI ordering.
     * </p>
     *
     * @return open project root paths as OS strings
     */
    public List<String> getOpenProjectRootPaths() {
        final var projects = getOpenProjectPaths();
        final var out = new ArrayList<String>(projects.size());
        for (final var path : projects) {
            if (path == null) {
                continue;
            }
            out.add(path.toString());
        }
        Collections.sort(out);
        return out;
    }

    private static List<Path> getOpenProjectPaths() {
        final var out = new ArrayList<Path>();
        try {
            final var root = ResourcesPlugin.getWorkspace().getRoot();
            for (var project : root.getProjects()) {
                if (project == null || !project.isOpen()) {
                    continue;
                }
                final var loc = project.getLocation();
                if (loc == null) {
                    continue;
                }
                out.add(Path.of(loc.toOSString()));
            }
        } catch (Exception e) {
            // ignore workspace discovery failures
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    private synchronized void ensureInstalled() {
        if (installed) {
            return;
        }
        installed = true;
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
                        IResourceChangeEvent.POST_CHANGE);
    }

    private void resourceChanged(IResourceChangeEvent e) {
        if (e == null) {
            return;
        }

        final var type = e.getType();

        if (type != IResourceChangeEvent.POST_CHANGE) {
            return;
        }

        final var delta = e.getDelta();
        if (delta == null) {
            return;
        }

        final var shouldRefresh = new AtomicBoolean(false);
        try {
            delta.accept(d -> {
                if (shouldRefresh.get()) {
                    return false;
                }
                final IResource resource = d.getResource();
                if (resource instanceof IProject) {
                    if ((d.getFlags() & IResourceDelta.OPEN) != 0) {
                        shouldRefresh.set(true);
                        return false;
                    }
                    final var kind = d.getKind();
                    if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
                        shouldRefresh.set(true);
                        return false;
                    }
                }
                return true;
            });
        } catch (Exception ex) {
            LOGGER.logWarning("Failed to visit workspace delta for workspace projects monitor", ex);
            shouldRefresh.set(true);
        }

        if (shouldRefresh.get()) {
            final var open = hasOpenProjects();
            notifyListeners(EventKind.PROJECTS_CHANGED, open);

            // here is the key: postpone the heavy work to make UI responsive
            debounceJob.cancel();
            if (!open) {
                return;
            }
            debounceJob.schedule(AUTO_REFRESH_DELAY_MS);
        }
    }

    private void notifyListeners(EventKind kind, boolean hasOpenProjects) {
        listeners.removeIf(ref -> ref == null || ref.get() == null);
        final var event = new Event(kind, hasOpenProjects);
        for (final var ref : listeners) {
            final var listener = ref.get();
            if (listener == null) {
                continue;
            }
            try {
                listener.onWorkspaceProjectsEvent(event);
            } catch (Exception e) {
                LOGGER.logWarning("Workspace projects listener failed", e);
            }
        }
    }
}
