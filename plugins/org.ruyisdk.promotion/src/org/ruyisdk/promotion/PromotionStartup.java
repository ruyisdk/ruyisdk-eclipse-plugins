package org.ruyisdk.promotion;

import java.util.concurrent.TimeUnit;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.promotion.dialogs.QuestionnaireDialog;
import org.ruyisdk.promotion.views.WebsiteView;

/**
 * Automatically opens the RuyiSDK Website View on IDE startup.
 */
public class PromotionStartup implements IStartup {

    private static final PluginLogger LOGGER = Activator.getLogger();
    private static final Activator ACTIVATOR = Activator.getDefault();
    private static final String QUESTIONNAIRE_REMIND_UNTIL_KEY = "questionnaire.remindUntilMillis";
    private static final long REMIND_DURATION_MILLIS = TimeUnit.DAYS.toMillis(1);

    @Override
    public void earlyStartup() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            final IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench.isClosing()) {
                return;
            }
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window == null && workbench.getWorkbenchWindows().length > 0) {
                window = workbench.getWorkbenchWindows()[0];
            }
            if (window != null) {
                final IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    try {
                        page.showView(WebsiteView.ID);
                    } catch (PartInitException e) {
                        LOGGER.logError("Failed to open RuyiSDK Website View", e);
                    }
                }
                if (shouldShowQuestionnaireDialog()) {
                    final int result = new QuestionnaireDialog(window.getShell()).open();
                    if (result == IDialogConstants.OK_ID) {
                        setQuestionnaireRemindUntil();
                    } else {
                        clearQuestionnaireRemindUntil();
                    }
                }
            }
        });
    }

    private boolean shouldShowQuestionnaireDialog() {
        if (ACTIVATOR == null) {
            return true;
        }
        final var remindUntilMillis =
                ACTIVATOR.getPreferenceStore().getLong(QUESTIONNAIRE_REMIND_UNTIL_KEY);
        return System.currentTimeMillis() >= remindUntilMillis;
    }

    private void setQuestionnaireRemindUntil() {
        if (ACTIVATOR != null) {
            final var remindUntilMillis = System.currentTimeMillis() + REMIND_DURATION_MILLIS;
            ACTIVATOR.getPreferenceStore().setValue(QUESTIONNAIRE_REMIND_UNTIL_KEY,
                    remindUntilMillis);
        }
    }

    private void clearQuestionnaireRemindUntil() {
        if (ACTIVATOR != null) {
            ACTIVATOR.getPreferenceStore().setValue(QUESTIONNAIRE_REMIND_UNTIL_KEY, 0L);
        }
    }
}
