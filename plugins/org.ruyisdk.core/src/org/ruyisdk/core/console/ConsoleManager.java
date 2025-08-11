package org.ruyisdk.core.console;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * 控制台生命周期管理
 */
public class ConsoleManager {
    
    public static void showConsole() {
        IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
        RuyiSdkConsole console = RuyiSdkConsole.getInstance();
        
        // 避免重复添加
        IConsole[] existing = consoleManager.getConsoles();
        for (IConsole c : existing) {
            if (c == console.getConsole()) {
                return;
            }
        }
        
        consoleManager.addConsoles(new IConsole[]{ console.getConsole() });
        consoleManager.showConsoleView(console.getConsole());
    }

    public static void dispose() {
        ConsolePlugin.getDefault().getConsoleManager()
            .removeConsoles(new IConsole[]{ RuyiSdkConsole.getInstance().getConsole() });
    }
}