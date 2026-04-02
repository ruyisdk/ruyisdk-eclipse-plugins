package org.ruyisdk.promotion.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/**
 * Embedded browser view for the RuyiSDK website. Uses the platform's native engine.
 */
public class WebsiteView extends ViewPart {

    public static final String ID = "org.ruyisdk.promotion.view";

    private static final String RUYISDK_URL = "https://ruyisdk.cn/";

    private Browser browser;
    private Text addressBar;
    private Button backButton;
    private Button forwardButton;

    @Override
    public void createPartControl(Composite parent) {
        final var container = new Composite(parent, SWT.NONE);
        final var layout = new GridLayout(1, false);
        {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        container.setLayout(layout);

        createToolbar(container);

        browser = new Browser(container, SWT.NONE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Only allow ruyisdk.cn navigation
        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                final var url = event.location;
                if (!isAllowedUrl(url)) {
                    event.doit = false;
                }
            }

            @Override
            public void changed(LocationEvent event) {
                if (event.top) {
                    syncAddressBar();
                }
            }
        });

        // JS-to-Java callback for SPA URL changes
        new BrowserFunction(browser, "_onRuyiSdkUrlChanged") {
            @Override
            public Object function(Object[] arguments) {
                syncAddressBar();
                return null;
            }
        };

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                syncAddressBar();
                injectNavigationHook();
            }
        });

        // TitleListener fires reliably on WebKit2 when a page finishes loading
        browser.addTitleListener(new TitleListener() {
            @Override
            public void changed(TitleEvent event) {
                syncAddressBar();
            }
        });

        browser.setUrl(RUYISDK_URL);
    }

    private void createToolbar(Composite parent) {
        final var toolbar = new Composite(parent, SWT.NONE);
        final var toolbarLayout = new GridLayout(6, false);
        {
            toolbarLayout.marginHeight = 2;
            toolbarLayout.marginWidth = 2;
        }
        toolbar.setLayout(toolbarLayout);
        toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        backButton = new Button(toolbar, SWT.PUSH);
        backButton.setText("◀");
        backButton.setToolTipText("Back");
        backButton.setEnabled(false);
        backButton.addListener(SWT.Selection, e -> browser.back());

        forwardButton = new Button(toolbar, SWT.PUSH);
        forwardButton.setText("▶");
        forwardButton.setToolTipText("Forward");
        forwardButton.setEnabled(false);
        forwardButton.addListener(SWT.Selection, e -> browser.forward());

        final var refreshButton = new Button(toolbar, SWT.PUSH);
        refreshButton.setText("↻");
        refreshButton.setToolTipText("Refresh");
        refreshButton.addListener(SWT.Selection, e -> browser.refresh());

        final var homeButton = new Button(toolbar, SWT.PUSH);
        homeButton.setText("⌂");
        homeButton.setToolTipText("Home");
        homeButton.addListener(SWT.Selection, e -> browser.setUrl(RUYISDK_URL));

        addressBar = new Text(toolbar, SWT.BORDER | SWT.SINGLE);
        addressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        addressBar.setText(RUYISDK_URL);
        addressBar.addListener(SWT.DefaultSelection, e -> {
            final var url = addressBar.getText().strip();
            if (isAllowedUrl(url)) {
                browser.setUrl(url);
            }
        });

        final var goButton = new Button(toolbar, SWT.PUSH);
        goButton.setText("Go");
        goButton.setToolTipText("Navigate to URL");
        goButton.addListener(SWT.Selection, e -> {
            final var url = addressBar.getText().strip();
            if (isAllowedUrl(url)) {
                browser.setUrl(url);
            }
        });
    }

    private void syncAddressBar() {
        if (addressBar != null && !addressBar.isDisposed()) {
            final var url = browser.getUrl();
            if (url != null && !url.equals(addressBar.getText())) {
                addressBar.setText(url);
            }
        }
        updateNavigationButtons();
    }

    /**
     * Injects JS to detect SPA URL changes. Prefers the Navigation API, falls back to pushState
     * monkey-patching.
     */
    private void injectNavigationHook() {
        browser.execute("""
                        if (!window._ruyiSdkNavHooked) {
                          window._ruyiSdkNavHooked = true;
                          if (window.navigation && typeof window.navigation.addEventListener === 'function') {
                            window.navigation.addEventListener('currententrychange', function() {
                              window._onRuyiSdkUrlChanged();
                            });
                          } else {
                            var origPush = history.pushState;
                            var origReplace = history.replaceState;
                            history.pushState = function() {
                              var r = origPush.apply(this, arguments);
                              window._onRuyiSdkUrlChanged();
                              return r;
                            };
                            history.replaceState = function() {
                              var r = origReplace.apply(this, arguments);
                              window._onRuyiSdkUrlChanged();
                              return r;
                            };
                            window.addEventListener('popstate', function() {
                              window._onRuyiSdkUrlChanged();
                            });
                          }
                        }
                        """);
    }

    private void updateNavigationButtons() {
        if (backButton != null && !backButton.isDisposed()) {
            backButton.setEnabled(browser.isBackEnabled());
        }
        if (forwardButton != null && !forwardButton.isDisposed()) {
            forwardButton.setEnabled(browser.isForwardEnabled());
        }
    }

    private static boolean isAllowedUrl(String url) {
        if (url == null) {
            return false;
        }

        // Allow ruyisdk.cn (+ subdomains) over HTTPS and a minimal internal URL (about:blank) only
        if ("about:blank".equals(url)) {
            return true;
        }

        // Only allow HTTPS navigation to ruyisdk.cn and its subdomains
        if (!url.startsWith("https://")) {
            return false;
        }

        return url.equals("https://ruyisdk.cn")
                || url.startsWith("https://ruyisdk.cn/")
                || url.matches("^https://[a-zA-Z0-9-]+\\.ruyisdk\\.cn(/.*)?$");
    }

    @Override
    public void setFocus() {
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
        }
    }
}
