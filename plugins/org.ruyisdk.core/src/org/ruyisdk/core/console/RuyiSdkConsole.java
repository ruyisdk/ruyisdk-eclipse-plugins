package org.ruyisdk.core.console;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * RuyiSDK 专属控制台（单例模式）
 */
public class RuyiSdkConsole {
    private static final String CONSOLE_NAME = "RuyiSDK";
    private static final String CONSOLE_TYPE = "org.ruyisdk.console";

    // private static final String CONSOLE_ID = "org.ruyisdk.core.console";
    // private static volatile RuyiSDKConsole instance;

    // 静态内部类实现懒加载单例
    private static class Holder {
        static final RuyiSdkConsole INSTANCE = new RuyiSdkConsole();
    }

    private final MessageConsole console;
    private MessageConsoleStream infoStream;
    private MessageConsoleStream warnStream;
    private MessageConsoleStream errorStream;
    private MessageConsoleStream commandStream;

    // 私有构造函数
    private RuyiSdkConsole() {
        this.console = new MessageConsole(CONSOLE_NAME, CONSOLE_TYPE, ImageDescriptor.getMissingImageDescriptor(),
                        true);

        // 初始化消息流
        initStreams();
    }

    /**
     * 获取单例实例（线程安全）
     */
    public static RuyiSdkConsole getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void logInfo(String message) {
        infoStream.println("[INFO] " + message);
    }

    public synchronized void logCommand(String message) {
        infoStream.println("[COMMAND] " + message);
    }

    public synchronized void logWarn(String message) {
        infoStream.println("[WARN] " + message);
    }

    public synchronized void logError(String message) {
        errorStream.println("[ERROR] " + message);
        errorStream.setActivateOnWrite(true); // 错误时自动激活控制台
    }

    public MessageConsole getConsole() {
        return console;
    }

    /**
     * 初始化各等级消息流
     */
    private void initStreams() {
        Display display = Display.getDefault();

        // INFO流（绿色）
        infoStream = console.newMessageStream();
        infoStream.setColor(new Color(display, 0, 127, 0));

        // WARN流（橙色）
        warnStream = console.newMessageStream();
        warnStream.setColor(new Color(display, 200, 100, 0));

        // ERROR流（红色）
        errorStream = console.newMessageStream();
        errorStream.setColor(new Color(display, 255, 0, 0));

        // COMMAND流（蓝色）
        commandStream = console.newMessageStream();
        commandStream.setColor(new Color(display, 0, 0, 255));
    }


}
