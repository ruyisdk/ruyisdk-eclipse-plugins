package org.ruyisdk.ruyi.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ruyisdk.ruyi.services.PackageIndexUpdater;

/**
 * Handler for the "Update Package Index" command. Runs {@code ruyi update} with a progress dialog.
 */
public class UpdatePackageIndexHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final var shell = HandlerUtil.getActiveShell(event);
        PackageIndexUpdater.updateWithProgress(shell);
        return null;
    }
}
