package org.ruyisdk.intro;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.ruyisdk.core.util.PluginLogger;

/**
 * Handler to open browser for various URLs.
 */
public class OpenBrowserHandler extends AbstractHandler {
    private static final PluginLogger LOGGER = Activator.getLogger();
    // URL Mapping Table
    private static final String[][] URL_MAPPINGS = {{"blog", "https://ruyisdk.org/blog"},
            {"home", "https://ruyisdk.org"}, {"contact", "https://ruyisdk.org/contact"},
            {"docs", "https://ruyisdk.org/docs/intro"}, {"matrix", "https://matrix.ruyisdk.org"},
            {"discussions", "https://github.com/ruyisdk/ruyisdk/discussions"}};

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Extract target type from command ID (eg:"org.ruyisdk.intro.openBrowser.docs" get the "docs" )
        String command = event.getCommand().getId();
        String target = command.substring(command.lastIndexOf('.') + 1);

        // Find URL
        String url = findUrl(target);
        if (url == null) {
            LOGGER.logWarning("No URL mapping found for: " + target);
            return null;
        }

        openUrl(url);
        return null;
    }

    private String findUrl(String target) {
        for (String[] mapping : URL_MAPPINGS) {
            if (mapping[0].equals(target)) {
                return mapping[1];
            }
        }
        return null;
    }

    void openUrl(String url) {
        try {
            // First level: Java standard API (most concise)
            java.awt.Desktop.getDesktop().browse(new URI(url));
            return;
        } catch (IOException | URISyntaxException e) {
            LOGGER.logWarning("Java Desktop open failed", e);
        }

        // L2: System commands (highest compatibility)
        try {
            String[] cmd;
            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                cmd = new String[] {"cmd", "/c", "start", url};
            } else if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
                cmd = new String[] {"open", url};
            } else {
                cmd = new String[] {"xdg-open", url};
            }
            new ProcessBuilder(cmd).inheritIO().start();
        } catch (IOException ex) {
            LOGGER.logError("All browser open methods failed", ex);
        }
    }

}
