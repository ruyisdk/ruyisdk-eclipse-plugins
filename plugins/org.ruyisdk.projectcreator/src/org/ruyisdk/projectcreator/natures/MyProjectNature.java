package org.ruyisdk.projectcreator.natures;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.ruyisdk.projectcreator.builder.MakefileBuilder;

public class MyProjectNature implements IProjectNature {

	public static final String NATURE_ID = "org.ruyisdk.projectcreator.projectNature";
	private IProject project;

	@Override
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		// Check if builder already exists
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(MakefileBuilder.BUILDER_ID)) {
				return;
			}
		}

		// Add new builder to project
		ICommand newCommand = desc.newCommand();
		newCommand.setBuilderName(MakefileBuilder.BUILDER_ID);

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		newCommands[commands.length] = newCommand;
		desc.setBuildSpec(newCommands);

		project.setDescription(desc, null);
	}

	@Override
	public void deconfigure() throws CoreException {
		// Remove builder when nature is removed
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(MakefileBuilder.BUILDER_ID)) {
				// Remove our builder
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				getProject().setDescription(description, null);
				return;
			}
		}
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
}