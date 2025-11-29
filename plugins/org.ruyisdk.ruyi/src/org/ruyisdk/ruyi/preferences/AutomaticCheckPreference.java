package org.ruyisdk.ruyi.preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.ruyisdk.core.ruyi.model.RepoConfig;
import org.ruyisdk.ruyi.services.RuyiProperties;

/**
 * Preference component for automatic detection.
 */
public class AutomaticCheckPreference {
    private final Composite parent;
    private Button automaticCheckCheckbox;

    /**
     * Constructs preference component.
     *
     * @param parent parent composite
     */
    public AutomaticCheckPreference(Composite parent) {
        this.parent = parent;
    }

    /**
     * Creates UI section.
     */
    public void createSection() {
        // checkbox
        automaticCheckCheckbox = new Button(parent, SWT.CHECK);
        automaticCheckCheckbox.setText("Automatically detect ruyi at startup");
        automaticCheckCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        automaticCheckCheckbox.setSelection(isAutomaticDetectionEnabled());
        // automaticCheckCheckbox.addListener(SWT.Selection, e -> setAutomaticDetection());
    }

    /**
     * Checks if automatic detection is enabled.
     *
     * @return true if enabled
     */
    public boolean isAutomaticDetectionEnabled() {
        return RuyiProperties.isAutomaticDetectionEnabled();
    }

    /**
     * Sets default detection state.
     *
     * <p>
     * Sets the default state of automatic detection to enabled.
     */
    public void defaultedAutomaticDetectionState() {
        automaticCheckCheckbox.setSelection(true);
    }

    /**
     * Saves automatic detection setting.
     *
     * <p>
     * Saves the current state of automatic detection.
     */
    public void setAutomaticDetection() {
        try {
            RuyiProperties.setAutomaticDetection(automaticCheckCheckbox.getSelection());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
