package org.ruyisdk.projectcreator.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.ruyisdk.projectcreator.Activator;

public class BuildPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	private Text buildCommandText;
	private Text ruyiVenvCmdText;
	private static final String BUILD_CMD_PROPERTY = "buildCmd";
	private static final String RUYI_VENV_CMD_PROPERTY = "ruyiVenvCmd";
	private static final String BOARD_MODEL_PROPERTY = "boardModel";

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NONE).setText("Compile Commands:");
		buildCommandText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		buildCommandText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new Label(composite, SWT.NONE).setText("Ruyi Venv Command:");
		ruyiVenvCmdText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		ruyiVenvCmdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		loadProperties();

		return composite;
	}

	private void loadProperties() {
		try {
			IProject project = getElement().getAdapter(IProject.class);

			String buildCmd = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BUILD_CMD_PROPERTY));
			if (buildCmd != null) {
				buildCommandText.setText(buildCmd);
			} else {
				buildCommandText.setText("make");
			}

			String venvCmd = project
					.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, RUYI_VENV_CMD_PROPERTY));
			if (venvCmd != null && !venvCmd.trim().isEmpty()) {
				ruyiVenvCmdText.setText(venvCmd);
			} else {

				ruyiVenvCmdText.setText(generateDefaultVenvCommand());
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performOk() {
		try {
			IProject project = getElement().getAdapter(IProject.class);

			project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BUILD_CMD_PROPERTY),
					buildCommandText.getText().trim());

			project.setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, RUYI_VENV_CMD_PROPERTY),
					ruyiVenvCmdText.getText().trim());
		} catch (CoreException e) {
			return false;
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {

		buildCommandText.setText("make");

		ruyiVenvCmdText.setText(generateDefaultVenvCommand());
		super.performDefaults();
	}

	private String generateDefaultVenvCommand() {
		try {
			IProject project = getElement().getAdapter(IProject.class);
			String boardModel = project
					.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BOARD_MODEL_PROPERTY));

			String toolchain;
			String profile;

			if (boardModel == null || boardModel.trim().isEmpty()) {

				toolchain = "gnu-plct";
				profile = "generic";
			} else {

				profile = boardModel;
				toolchain = org.ruyisdk.packages.JsonParser.findInstalledToolchainForBoard(boardModel);
				if (toolchain == null || toolchain.trim().isEmpty()) {
					if ("milkv-duo".equals(boardModel)) {
						toolchain = "gnu-milkv-milkv-duo-elf-bin";
					} else if ("sipeed-lpi4a".equals(boardModel)) {
						toolchain = "gnu-plct-xthead";
					} else {

						toolchain = "gnu-plct";
					}
				} else {
					int versionIndex = toolchain.indexOf("-0.");
					if (versionIndex > 0) {
						toolchain = toolchain.substring(0, versionIndex);
					}
				}
			}

			String ruyiPath = getRuyiInstallPath() + "/ruyi";

			return String.format("%s venv -t %s %s ./%s", ruyiPath, toolchain, profile, "ruyiVenv");

		} catch (CoreException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String getRuyiInstallPath() {
		try {
			Class<?> clazz = Class.forName("org.ruyisdk.ruyi.util.RuyiFileUtils");
			java.lang.reflect.Method method = clazz.getMethod("getInstallPath");
			return (String) method.invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
			return System.getProperty("user.home") + "/.ruyi";
		}
	}
}