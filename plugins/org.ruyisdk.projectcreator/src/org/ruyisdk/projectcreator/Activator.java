package org.ruyisdk.projectcreator;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.core.util.PluginLogger;

/**
 * Activator for the project creator plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.projectcreator";
    private static final PluginLogger LOGGER =
            new PluginLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);
    private static Activator plugin;
    private IResourceChangeListener buildListener;

    /**
     * Starts the plugin.
     *
     * @param context bundle context
     * @throws Exception if start fails
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        getBundle().loadClass("org.ruyisdk.projectcreator.builder.MakefileBuilder");
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        buildListener = event -> {
            switch (event.getType()) {
                case IResourceChangeEvent.PRE_BUILD:
                    if (event.getSource() instanceof IProject) {
                        IProject project = (IProject) event.getSource();
                        LOGGER.logInfo("Building project: " + project.getName());
                        try {
                            IProjectDescription desc = project.getDescription();
                            ICommand[] commands = desc.getBuildSpec();
                            LOGGER.logInfo("Project builders:");
                            for (ICommand cmd : commands) {
                                LOGGER.logInfo("  - " + cmd.getBuilderName());
                            }
                        } catch (CoreException e) {
                            LOGGER.logError("Failed to add Makefile builder: " + e.getMessage(), e);
                        }
                    }
                    break;
                default:
                    break;
            }
        };
        workspace.addResourceChangeListener(buildListener, IResourceChangeEvent.PRE_BUILD
                | IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.PRE_DELETE);

        LOGGER.logInfo("Build listener registered successfully");
    }

    /**
     * Stops the plugin.
     *
     * @param context bundle context
     * @throws Exception if stop fails
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        if (buildListener != null) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(buildListener);
            LOGGER.logInfo("Build listener unregistered");
        }
        plugin = null;
        super.stop(context);
    }

    /** Returns the shared instance. */
    public static Activator getDefault() {
        return plugin;
    }

    /** Returns an Eclipse builtin logger. */
    public static PluginLogger getLogger() {
        return LOGGER;
    }
}
