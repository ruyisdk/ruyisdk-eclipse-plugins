


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
    // private URL baseHrefUrlForListener; // 不再需要这个成员变量了

    @Override
    public void init(IIntroSite site, IMemento memento) throws PartInitException {
        this.site = site;
    }

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new FillLayout());
        browser = new Browser(parent, SWT.NONE);
        System.out.println("DEBUG: CustomIntroPart: Browser control created.");

        try {
            Bundle bundle = Activator.getDefault() != null ? Activator.getDefault().getBundle() : FrameworkUtil.getBundle(getClass());
            if (bundle == null) {
                System.err.println("DEBUG: CustomIntroPart: CRITICAL - Bundle is null. Cannot locate resources.");
                browser.setText("<html><body><h1>Error: Plugin bundle could not be determined.</h1></body></html>");
                return;
            }
            System.out.println("DEBUG: CustomIntroPart: Using bundle: " + bundle.getSymbolicName() + " (State: " + bundle.getState() + ")");

            // 列出所有期望被加载的资源相对于 bundle 根目录的路径
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
                // 添加 welcome.html 中引用的所有其他图片
            );

            URL resolvedWelcomePageURL = null;

            System.out.println("DEBUG: CustomIntroPart: Attempting to 'touch' (resolve) all necessary resources to encourage extraction...");
            for (String resourcePath : resourcePaths) {
                URL bundleUrl = FileLocator.find(bundle, new Path(resourcePath), null);
                if (bundleUrl != null) {
                    try {
                        URL fileUrl = FileLocator.toFileURL(bundleUrl); // 这应该会触发解压到 .cp 目录
                        System.out.println("DEBUG: CustomIntroPart: Successfully resolved resource '" + resourcePath + "' to: " + fileUrl.toExternalForm());
                        if (resourcePath.equals("html/welcome.html")) {
                            resolvedWelcomePageURL = fileUrl; // 保存 welcome.html 的 file: URL
                        }
                    } catch (IOException e_resolve) {
                        System.err.println("DEBUG: CustomIntroPart: Failed to resolve resource '" + resourcePath + "' using FileLocator.toFileURL(). Error: " + e_resolve.getMessage());
                        // 即使某个资源解析失败，也继续尝试其他资源，但主 HTML 必须成功
                        if (resourcePath.equals("html/welcome.html")) {
                             System.err.println("DEBUG: CustomIntroPart: CRITICAL - Failed to resolve welcome.html. Cannot proceed.");
                             browser.setText("<html><body><h1>Error: Could not resolve welcome.html to a file URL.</h1><p>" + e_resolve.getMessage() + "</p></body></html>");
                             return;
                        }
                    }
                } else {
                    System.err.println("DEBUG: CustomIntroPart: Resource NOT FOUND in bundle: " + resourcePath);
                     if (resourcePath.equals("html/welcome.html")) {
                         System.err.println("DEBUG: CustomIntroPart: CRITICAL - welcome.html not found in bundle. Cannot proceed.");
                         browser.setText("<html><body><h1>Error: welcome.html not found in bundle.</h1></body></html>");
                         return;
                     }
                }
            }

            if (resolvedWelcomePageURL != null) {
                System.out.println("INFO: CustomIntroPart: All (attempted) resources touched. Setting browser URL to resolved welcome.html: " + resolvedWelcomePageURL.toExternalForm());
                browser.setUrl(resolvedWelcomePageURL.toExternalForm()); // 使用指向解压后 welcome.html 的 file: URL
            } else {
                System.err.println("DEBUG: CustomIntroPart: CRITICAL - resolvedWelcomePageURL is null after attempting to touch all resources. This should not happen if welcome.html was found.");
                browser.setText("<html><body><h1>Error: Welcome page URL could not be determined after resource processing.</h1></body></html>");
            }

        } catch (Exception e) { // 捕获其他意外错误
            System.err.println("DEBUG: CustomIntroPart: General error occurred while loading welcome page.");
            e.printStackTrace(); 
            browser.setText("<html><body><h1>Error loading welcome page.</h1><p>" + e.getMessage() + "</p></body></html>");
        }

        browser.addLocationListener(new LocationListener() {
            @Override
            public void changing(LocationEvent event) {
                String url = event.location;
                System.out.println("LocationListener: changing - URL: " + url); 

                if (url == null) {
                    System.out.println("LocationListener: URL is null, doing nothing.");
                    event.doit = false; 
                    return;
                }
                
                // 如果 URL 是 file:///.../.cp/... 形式，说明是内部资源，允许加载
                if (url.startsWith("file:") && url.contains(".cp/")) {
                     System.out.println("LocationListener: URL is an internal file resource. Allowing: " + url);
                     event.doit = true;
                     return;
                }
                // about:blank 是浏览器内部页面，允许
                if ("about:blank".equalsIgnoreCase(url)) {
                    System.out.println("LocationListener: URL is about:blank. Allowing: " + url);
                    event.doit = true;
                    return;
                }


                if (url.startsWith("http://org.eclipse.ui.intro/execute?command=")) {
                    System.out.println("LocationListener: Handling internal Eclipse command."); 
                    String commandId = url.substring(url.indexOf("command=") + "command=".length());
                    if (commandId.contains("&")) {
                        commandId = commandId.substring(0, commandId.indexOf("&"));
                    }
                    try {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            IHandlerService handlerService = window.getService(IHandlerService.class);
                            if (handlerService != null) {
                                System.out.println("LocationListener: Executing command: " + commandId); 
                                handlerService.executeCommand(commandId, null);
                            } else {
                                System.err.println("LocationListener: HandlerService is null for command: " + commandId);
                            }
                        } else {
                             System.err.println("LocationListener: ActiveWorkbenchWindow is null for command: " + commandId);
                        }
                    } catch (Exception e) {
                        System.err.println("LocationListener: Error executing command: " + commandId);
                        e.printStackTrace();
                    }
                    event.doit = false;
                } else if (url.startsWith("http:") || url.startsWith("https:")) {
                    System.out.println("LocationListener: Handling external HTTP/HTTPS link: " + url);
                    try {
                        System.out.println("LocationListener: Attempting to open in external browser: " + url);
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
                        System.out.println("LocationListener: External browser openURL call succeeded for: " + url);
                    } catch (PartInitException | MalformedURLException e) {
                        System.err.println("LocationListener: Error opening external browser for URL: " + url);
                        e.printStackTrace();
                    }
                    event.doit = false;
                } else {
                    // 对于其他 file:// URL (非 .cp/ 目录的) 或未知的协议，也阻止，除非明确允许
                    System.out.println("LocationListener: URL protocol not explicitly handled or not a .cp/ file. Blocking: " + url);
                    event.doit = false; 
                }
            }

            @Override
            public void changed(LocationEvent event) {
                System.out.println("LocationListener: changed - URL: " + event.location + ", top: " + event.top); 
            }
        });
        System.out.println("CustomIntroPart: LocationListener added to browser.");
    }

    // ... (其他方法保持不变) ...

    @Override
    public void standbyStateChanged(boolean standby) {
        // Handle standby state
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