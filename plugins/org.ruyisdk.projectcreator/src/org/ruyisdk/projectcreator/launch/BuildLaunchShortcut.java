package org.ruyisdk.projectcreator.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.ruyisdk.projectcreator.Activator;


import static org.ruyisdk.projectcreator.launch.LaunchConstants.*;

public class BuildLaunchShortcut implements ILaunchShortcut {

    @Override
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof IProject) {
                launchProject((IProject) element, mode);
            }
        }
    }

    @Override
    public void launch(IEditorPart editor, String mode) {

    }

    private void launchProject(IProject project, String mode) {
        try {
            ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType type =
                            launchManager.getLaunchConfigurationType(ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);

            if (type == null) {

                System.err.println("Launch configuration type not found: " + ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
                return;
            }

            String configName = launchManager.generateLaunchConfigurationName("Build " + project.getName());
            ILaunchConfigurationWorkingCopy wc = type.newInstance(null, configName);

            String toolchainPath =
                            project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, "toolchainPath"));
            String buildCmd = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, "buildCmd"));

            if (buildCmd == null || toolchainPath == null) {

                return;
            }


            String command = "make";
            String args = "";
            int firstSpace = buildCmd.indexOf(' ');
            if (firstSpace > 0) {
                args = buildCmd.substring(firstSpace + 1);
            }

            String commandPath = toolchainPath + "/bin/" + command;


            wc.setAttribute(ATTR_LOCATION, commandPath);
            wc.setAttribute(ATTR_WORKING_DIRECTORY, project.getLocation().toOSString());
            wc.setAttribute(ATTR_TOOL_ARGUMENTS, args);
            wc.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);

            ILaunchConfiguration config = wc.doSave();
            DebugUITools.launch(config, mode);

        } catch (CoreException e) {

            e.printStackTrace();
        }
    }
}
