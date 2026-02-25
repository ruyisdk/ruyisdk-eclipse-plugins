package org.ruyisdk.ruyi.services;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

/**
 * Service for launching interactive device provisioning in Eclipse terminal view.
 */
public class DeviceProvisionService {
    /**
     * Launches `ruyi device provision` in Eclipse built-in terminal. The terminal will be reused.
     *
     * @throws IOException if terminal launch fails
     */
    public void launchProvisionWizard() throws IOException {
        final var ruyiExecutable = toSingleQuoted(resolveRuyiExecutable());

        final var terminalService = TerminalServiceFactory.getService();
        if (terminalService == null) {
            throw new IOException("Eclipse terminal service is unavailable");
        }

        final var cmdline = new StringBuilder();
        cmdline.append("-l -c \"");
        cmdline.append("echo '------------';");
        cmdline.append("echo 'Docs: https://ruyisdk.org/en/docs/category/使用案例';");
        cmdline.append("echo 'Press \\\"Enter\\\" to re-launch ruyi after exiting it.';");
        cmdline.append("echo;");
        cmdline.append("echo 'Launching \\\"'").append(ruyiExecutable).append("'\\\"...';");
        cmdline.append("echo '------------';");
        cmdline.append("exec ").append(ruyiExecutable).append(" device provision");
        cmdline.append("\"");

        final var properties = new HashMap<String, Object>();
        properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
                        "org.eclipse.tm.terminal.connector.process.launcher.process");
        properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Ruyi Device Provision");
        properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, Boolean.FALSE);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "bash");
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, cmdline.toString());
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, System.getProperty("user.home"));

        terminalService.openConsole(properties, null);
    }

    private static String resolveRuyiExecutable() throws IOException {
        final var installPath = RuyiFileUtils.getInstallPath();
        if (installPath == null || installPath.isBlank()) {
            throw new IOException("ruyi not found");
        }

        final var fullPath = installPath + File.separator + "ruyi";
        final var executable = new File(fullPath);
        if (executable.isFile() && executable.canExecute()) {
            return fullPath;
        }

        throw new IOException("ruyi not found");
    }

    private static String toSingleQuoted(String value) {
        return "'" + value.replace("'", "'\\\"'\\\"'") + "'";
    }
}
