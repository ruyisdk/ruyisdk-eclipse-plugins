package org.ruyisdk.projectcreator;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator for the project creator plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.projectcreator";
    private static Activator plugin;
    private IResourceChangeListener buildListener;

    /**
     * Constructs the activator.
     */
    public Activator() {}

    /**
     * Starts the plugin.
     *
     * @param context bundle context
     * @throws Exception if start fails
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        try {
            Class<?> builderClass = getBundle().loadClass("org.ruyisdk.projectcreator.builder.MakefileBuilder");
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            buildListener = event -> {
                switch (event.getType()) {
                    case IResourceChangeEvent.PRE_BUILD:
                        if (event.getSource() instanceof IProject) {
                            IProject project = (IProject) event.getSource();
                            System.out.println(">>> Activator: Building project: " + project.getName());
                            try {
                                IProjectDescription desc = project.getDescription();
                                ICommand[] commands = desc.getBuildSpec();
                                System.out.println(">>> Activator: Project builders:");
                                for (ICommand cmd : commands) {
                                    System.out.println(">>>   - " + cmd.getBuilderName());
                                }
                            } catch (CoreException e) {
                                System.err.println(">>> Activator: Failed to add Makefile builder: " + e.getMessage());
                            }
                        }
                        break;
                    default:
                        break;
                }
            };
            workspace.addResourceChangeListener(buildListener, IResourceChangeEvent.PRE_BUILD
                            | IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.PRE_DELETE);

            System.out.println(">>> Activator: Build listener registered successfully");

        } catch (Exception e) {
            System.err.println(">>> Activator: Error during plugin initialization!");
            e.printStackTrace();
        }
    }

    /**
     * Stops the plugin.
     *
     * @param context bundle context
     * @throws Exception if stop fails
     */
    public void stop(BundleContext context) throws Exception {
        if (buildListener != null) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(buildListener);
            System.out.println(">>> Activator: Build listener unregistered");
        }
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }
}
