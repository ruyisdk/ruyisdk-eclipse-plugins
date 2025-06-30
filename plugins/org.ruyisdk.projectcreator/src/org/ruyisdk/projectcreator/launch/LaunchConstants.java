package org.ruyisdk.projectcreator.launch;

public final class LaunchConstants {

	/** The launch configuration type id for local external tools. */
	public static final String ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.ui.externaltools.ProgramLaunchConfigurationType";

	/**
	 * Launch configuration attribute key. The value is the location of an external
	 * tool.
	 */
	public static final String ATTR_LOCATION = "org.eclipse.ui.externaltools.ATTR_LOCATION";

	/**
	 * Launch configuration attribute key. The value is the working directory of an
	 * external tool.
	 */
	public static final String ATTR_WORKING_DIRECTORY = "org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY";

	/**
	 * Launch configuration attribute key. The value is the arguments to be passed
	 * to an external tool.
	 */
	public static final String ATTR_TOOL_ARGUMENTS = "org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS";

	private LaunchConstants() {
		// prevent instantiation
	}
}