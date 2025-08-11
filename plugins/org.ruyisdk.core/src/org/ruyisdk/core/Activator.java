package org.ruyisdk.core;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.core.console.ConsoleExtensions;
import org.ruyisdk.core.console.ConsoleManager;

/**
 * RuyiSDK Core 插件入口
 */
public class Activator extends AbstractUIPlugin {
    // 插件ID（需与MANIFEST.MF一致）
    public static final String PLUGIN_ID = "org.ruyisdk.core";

    // 单例实例
    private static Activator plugin;

    /**
     * 构造函数（必须公开无参）
     */
    public Activator() {
        // 父类AbstractUIPlugin会自动初始化
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // 初始化控制台扩展
        ConsoleExtensions.loadExtensions();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ConsoleManager.dispose();

        plugin = null;
        super.stop(context);
    }

    /**
     * 获取共享实例
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * 获取图像描述符（安全方式）
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
