package org.ruyisdk.intro;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
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

    @Override
    public void init(IIntroSite site, IMemento memento) throws PartInitException {
        this.site = site;
    }

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new FillLayout());
        browser = new Browser(parent, SWT.NONE);
        System.out.println("CustomIntroPart: Browser control created."); // 调试输出

        try {
            Bundle bundle = Activator.getDefault() != null ? Activator.getDefault().getBundle() : FrameworkUtil.getBundle(getClass());
            URL fileURL = FileLocator.find(bundle, new Path("html/welcome.html"), null);
            if (fileURL != null) {
                URL resolvedFileURL = FileLocator.toFileURL(fileURL);
                System.out.println("CustomIntroPart: Loading HTML from: " + resolvedFileURL.toExternalForm()); // 调试输出
                browser.setUrl(resolvedFileURL.toExternalForm());
            } else {
                System.err.println("CustomIntroPart: Error - welcome.html not found."); // 调试输出
                browser.setText("<html><body><h1>Error: Welcome page content (welcome.html) not found.</h1></body></html>");
            }
        } catch (Exception e) {
            System.err.println("CustomIntroPart: Error loading welcome page."); // 调试输出
            e.printStackTrace(); // 打印完整的异常堆栈
            browser.setText("<html><body><h1>Error loading welcome page.</h1><p>" + e.getMessage() + "</p></body></html>");
        }

        browser.addLocationListener(new LocationListener() {
            @Override
            public void changing(LocationEvent event) {
                String url = event.location;
                System.out.println("LocationListener: changing - URL: " + url); // 调试输出

                if (url == null) {
                    System.out.println("LocationListener: URL is null, doing nothing.");
                    event.doit = false; // 通常 URL 不应为 null，阻止以防万一
                    return;
                }

                if (url.startsWith("http://org.eclipse.ui.intro/execute?command=")) {
                    System.out.println("LocationListener: Handling internal Eclipse command."); // 调试输出
                    String commandId = url.substring(url.indexOf("command=") + "command=".length());
                    if (commandId.contains("&")) {
                        commandId = commandId.substring(0, commandId.indexOf("&"));
                    }
                    try {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            IHandlerService handlerService = window.getService(IHandlerService.class);
                            if (handlerService != null) {
                                System.out.println("LocationListener: Executing command: " + commandId); // 调试输出
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
                    System.out.println("LocationListener: Handling external HTTP/HTTPS link."); // 调试输出
                    try {
                        System.out.println("LocationListener: Attempting to open in external browser: " + url); // 调试输出
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
                        System.out.println("LocationListener: External browser openURL call succeeded for: " + url); // 调试输出
                    } catch (PartInitException | MalformedURLException e) {
                        System.err.println("LocationListener: Error opening external browser for URL: " + url);
                        e.printStackTrace();
                    }
                    event.doit = false;
                } else {
                    System.out.println("LocationListener: URL not an internal command or external HTTP/S link. Allowing default behavior (event.doit=true): " + url); // 调试输出
                    // 对于其他类型的 URL (例如 about:blank, file://), 允许内部浏览器正常处理
                    // event.doit = true; // 默认就是 true，可以不写
                }
            }

            @Override
            public void changed(LocationEvent event) {
                System.out.println("LocationListener: changed - URL: " + event.location + ", top: " + event.top); // 调试输出
            }
        });
        System.out.println("CustomIntroPart: LocationListener added to browser."); // 调试输出
    }

    // ... (其他方法 getTitle, getTitleImage, getAdapter 等保持不变) ...
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
        if (adapter.isAssignableFrom(IWorkbenchPart.class)) {
            return (T) this;
        }
        return null;
    }
}