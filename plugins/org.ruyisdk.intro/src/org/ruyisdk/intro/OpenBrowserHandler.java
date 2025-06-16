package org.ruyisdk.intro;

import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

public class OpenBrowserHandler extends AbstractHandler {
	// URL Mapping Table
    private static final String[][] URL_MAPPINGS = {
        {"blog",    "https://ruyisdk.org/blog"},
        {"home",    "https://ruyisdk.org"},
        {"contact", "https://ruyisdk.org/contact"},
        {"docs",    "https://ruyisdk.org/docs/intro"},
        {"matrix",  "https://matrix.ruyisdk.org"},
        {"discussions",  "https://github.com/ruyisdk/ruyisdk/discussions"}
        
    };
    
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
            if (mapping[0].equals(target)) return mapping[1];
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
                cmd = new String[]{"cmd", "/c", "start", url};
            } else if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
                cmd = new String[]{"open", url};
            } else {
                cmd = new String[]{"xdg-open", url};
            }
            new ProcessBuilder(cmd).inheritIO().start();
        } catch (IOException ex) {
            System.err.println("All methods failed: " + ex.getMessage());
        }
    }
    
    // Using the Eclipse browser mechanism (Window → Preferences → General → Web Browser)
    private void openBrowserSupportUrl(String url) {
        try {
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            browserSupport.getExternalBrowser().openURL(new URL(url));
            System.out.println("Successfully opened: " + url);
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }
    
    private void openUrlAbsolutely(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            ProcessBuilder pb = new ProcessBuilder();

            if (os.contains("win")) {
                pb.command("cmd", "/c", "start", "msedge", url);  // Force Edge
            } else if (os.contains("mac")) {
                pb.command("open", "-a", "Safari", url);         // Force Safari
            } else {
                pb.command("sh", "-c", "xdg-open '" + url + "' || sensible-browser '" + url + "'");
            }

            // Key configuration: inherit environment variables + redirect error stream
            pb.inheritIO()
              .redirectErrorStream(true)
              .start();
            
        } catch (IOException e) {
            System.err.println("System call method failed to open webpage: " + e.getMessage());
        }
    }
}