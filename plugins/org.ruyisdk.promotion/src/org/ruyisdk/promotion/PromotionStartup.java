package org.ruyisdk.promotion;

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

                new QuestionnaireDialog(window.getShell()).open();
            }
        });
    }
}
