package org.ruyisdk.projectcreator.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class BoardSelectionPage extends WizardPage {

	private Combo boardModelCombo;

	protected BoardSelectionPage(String pageName) {
		super(pageName);
		setTitle("choose Board Model");
		setDescription("Please select your target board model.");
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(2, false));

		new Label(container, SWT.NULL).setText("Board Model:");
		boardModelCombo = new Combo(container, SWT.READ_ONLY);
		boardModelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		String[] boardModels = new String[] { "milkv-duo", "default" };
		boardModelCombo.setItems(boardModels);

		boardModelCombo.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				validatePage();
			}
		});
	}

	private void validatePage() {
		if (boardModelCombo.getText().isEmpty()) {
			setErrorMessage("Please select a board model.");
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	public String getBoardModel() {
		return boardModelCombo.getText();
	}
}
