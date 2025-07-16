package org.ruyisdk.ruyi.preferences;

import java.io.IOException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.ruyisdk.ruyi.services.RuyiProperties;

public class AutomaticCheckPreference {
	private final Composite parent;
	private Button automaticCheckCheckbox;

	public AutomaticCheckPreference(Composite parent) {
		this.parent = parent;
	}

	public void createSection() {
		// checkbox
		automaticCheckCheckbox = new Button(parent, SWT.CHECK);
		automaticCheckCheckbox.setText("Automatically detect ruyi at startup");
		automaticCheckCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		automaticCheckCheckbox.setSelection(isAutomaticDetectionEnabled());
//		automaticCheckCheckbox.addListener(SWT.Selection, e -> setAutomaticDetection());
	}

	public boolean isAutomaticDetectionEnabled() {
		return RuyiProperties.isAutomaticDetectionEnabled();
	}

	public void defaultedAutomaticDetectionState() {
		automaticCheckCheckbox.setSelection(true);
	}
	
    // 保存"不再提示"设置
	public void setAutomaticDetection() {
       try {
			RuyiProperties.setAutomaticDetection(automaticCheckCheckbox.getSelection());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}