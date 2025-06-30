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
	private static final String BUILD_CMD_PROPERTY = "buildCmd";

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NONE).setText("Compile Commands:");
		buildCommandText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		buildCommandText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		loadProperties();

		return composite;
	}

	private void loadProperties() {
		try {
			IProject project = getElement().getAdapter(IProject.class);
			String buildCmd = project.getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID, BUILD_CMD_PROPERTY));
			if (buildCmd != null) {
				buildCommandText.setText(buildCmd);
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
					buildCommandText.getText());
		} catch (CoreException e) {
			return false;
		}
		return super.performOk();
	}
}