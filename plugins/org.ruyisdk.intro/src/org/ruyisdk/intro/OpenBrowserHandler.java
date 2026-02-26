package org.ruyisdk.intro;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Handler to open browser for various URLs.
 */
public class OpenBrowserHandler extends AbstractHandler {
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
            System.err.println("No URL mapping found for: " + target);
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
        } catch (Exception e) {
            System.err.println("Java Desktop open failed : " + e.getMessage());
            System.out.println("Desktop method failed, falling back to command invocation");
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
            System.err.println("All methods failed: " + ex.getMessage());
        }
    }

}
