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

public class OpenBrowserHandler extends AbstractHandler {
	// URL映射表
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
    	// 从命令ID中提取目标类型(eg:"org.ruyisdk.intro.openBrowser.docs" get the "docs" )
        String command = event.getCommand().getId();
        String target = command.substring(command.lastIndexOf('.') + 1);
        
        // 查找URL
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
            // 第一级：Java标准API（最简洁）
            java.awt.Desktop.getDesktop().browse(new URI(url));
            return;
        } catch (Exception e) {
            System.err.println("Java Desktop open failed : " + e.getMessage());
            System.out.println("Desktop方式失败，降级到命令调用");
        }

        // 第二级：系统命令（最高兼容性）
        try {
            String[] cmd;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                cmd = new String[]{"cmd", "/c", "start", url};
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                cmd = new String[]{"open", url};
            } else {
                cmd = new String[]{"xdg-open", url};
            }
            new ProcessBuilder(cmd).inheritIO().start();
        } catch (IOException ex) {
            System.err.println("所有方式均失败: " + ex.getMessage());
        }
    }
    
    // 使用 Eclipse 浏览器机制(Window → Preferences → General → Web Browser)
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
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = new ProcessBuilder();

            if (os.contains("win")) {
                pb.command("cmd", "/c", "start", "msedge", url);  // 强制指定Edge
            } else if (os.contains("mac")) {
                pb.command("open", "-a", "Safari", url);         // 强制指定Safari
            } else {
                pb.command("sh", "-c", "xdg-open '" + url + "' || sensible-browser '" + url + "'");
            }

            // 关键配置：继承环境变量 + 重定向错误流
            pb.inheritIO()
              .redirectErrorStream(true)
              .start();
            
        } catch (IOException e) {
            System.err.println("系统调用方式打卡网页失败: " + e.getMessage());
        }
    }
}