package org.ruyisdk.intro;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CustomIntroPart implements IIntroPart {
    private Browser browser;
    private IIntroSite site;
    private List<IPropertyListener> propertyListeners = new ArrayList<>();
    private String title = "Welcome";

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    @Override
    public void init(IIntroSite site, IMemento memento) throws PartInitException {
        this.site = site;
    }

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new FillLayout());
        browser = new Browser(parent, SWT.NONE);

        try {
            Bundle bundle = Activator.getDefault() != null ? Activator.getDefault().getBundle() : FrameworkUtil.getBundle(getClass());
            if (bundle == null) {
                browser.setText("<html><body><h1>Error: Plugin bundle could not be determined.</h1></body></html>");
                return;
            }

            List<String> resourcePaths = Arrays.asList(
                "html/welcome.html",
                "html/style.css",
                "icons/ruyi_logo.png",
                "icons/icon_new.png",
                "icons/icon_open.png",
                "icons/icon_settings.png",
                "icons/icon_matrix.png",
                "icons/icon_docs.png",
                "icons/icon_discussions.png"
                // Add all other images referenced in welcome.html here
            );

            URL resolvedWelcomePageURL = null;

            for (String resourcePath : resourcePaths) {
                URL bundleUrl = FileLocator.find(bundle, new Path(resourcePath), null);
                if (bundleUrl != null) {
                    try {
                        URL fileUrl = FileLocator.toFileURL(bundleUrl); 
                        if (resourcePath.equals("html/welcome.html")) {
                            resolvedWelcomePageURL = fileUrl; 
                        }
                    } catch (IOException e_resolve) {
                        if (resourcePath.equals("html/welcome.html")) {
                             String errorMessage = e_resolve.getMessage();
                             String safeErrorMessage = (errorMessage == null) ? "An unknown error occurred." : escapeHtml(errorMessage);
                             browser.setText("<html><body><h1>Error: Could not resolve welcome.html to a file URL.</h1><p>" + safeErrorMessage + "</p></body></html>");
                             return;
                        }
                        // For other resources, we might log this error but not necessarily stop loading the intro
                    }
                } else {
                     if (resourcePath.equals("html/welcome.html")) {
                         browser.setText("<html><body><h1>Error: welcome.html not found in bundle.</h1></body></html>");
                         return;
                     }
                     // For other resources, we might log this error
                }
            }

            if (resolvedWelcomePageURL != null) {
                browser.setUrl(resolvedWelcomePageURL.toExternalForm()); 
            } else {
                browser.setText("<html><body><h1>Error: Welcome page URL could not be determined after resource processing.</h1></body></html>");
            }

        } catch (Exception e) { 
            String errorMessage = e.getMessage();
            String safeErrorMessage = (errorMessage == null) ? "An unknown error occurred." : escapeHtml(errorMessage);
            browser.setText("<html><body><h1>Error loading welcome page.</h1><p>" + safeErrorMessage + "</p></body></html>");
            // Consider proper logging for 'e' here using Activator or ILog
        }

        browser.addLocationListener(new LocationListener() {
            @Override
            public void changing(LocationEvent event) {
                String url = event.location;

                if (url == null) {
                    event.doit = false; 
                    return;
                }
                
                if (url.startsWith("file:") && url.contains(".cp/")) {
                     event.doit = true;
                     return;
                }
                
                if ("about:blank".equalsIgnoreCase(url)) {
                    event.doit = true;
                    return;
                }

                if (url.startsWith("http://org.eclipse.ui.intro/execute?command=")) {
                    String commandId = url.substring(url.indexOf("command=") + "command=".length());
                    if (commandId.contains("&")) {
                        commandId = commandId.substring(0, commandId.indexOf("&"));
                    }
                    try {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            IHandlerService handlerService = window.getService(IHandlerService.class);
                            if (handlerService != null) {
                                handlerService.executeCommand(commandId, null);
                            } 
                        }
                    } catch (Exception e) {
                        // Consider proper logging for 'e' here
                    }
                    event.doit = false;
                } else if (url.startsWith("http:") || url.startsWith("https:")) {
                    try {
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
                    } catch (PartInitException | MalformedURLException e) {
                        // Consider proper logging for 'e' here
                    }
                    event.doit = false;
                } else if (url.startsWith("file:")) { 
                    event.doit = true; 
                } else {
                    event.doit = false; 
                }
            }

            @Override
            public void changed(LocationEvent event) {
                // Nothing to do here for now
            }
        });
    }

    @Override
    public void standbyStateChanged(boolean standby) {
        // Handle standby state if needed
    }

    @Override
    public void setFocus() {
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        propertyListeners.clear();
        this.site = null;
    }

    @Override
    public void addPropertyListener(IPropertyListener listener) {
        if (!propertyListeners.contains(listener)) {
            propertyListeners.add(listener);
        }
    }

    @Override
    public void removePropertyListener(IPropertyListener listener) {
        propertyListeners.remove(listener);
    }

    @Override
    public IIntroSite getIntroSite() {
        return this.site;
    }

    @Override
    public void saveState(IMemento memento) {
        // No specific state to save for this simple intro
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public Image getTitleImage() {
        return null; // No title image by default
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(IIntroPart.class)) {
            return (T) this;
        }
        return null;
    }
}