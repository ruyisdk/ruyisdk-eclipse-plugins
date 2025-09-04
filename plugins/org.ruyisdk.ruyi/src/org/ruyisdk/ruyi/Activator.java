package org.ruyisdk.ruyi;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.ruyisdk.ruyi.core.RuyiCore;
import org.ruyisdk.ruyi.util.RuyiLogger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.ruyi";
    private static Activator plugin; // 共享实例
    private RuyiCore ruyiCore; // 核心服务
    private RuyiLogger logger;

    public Activator() {
        // 保证单例
        if (plugin != null) {
            throw new IllegalStateException("Activator already initialized");
        }
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        // plugin = this;

        try {
            // 1. 初始化日志系统
            logger = new RuyiLogger(getLog());

            // 2. 加载默认首选项
            // RuyiPreferenceInitializer.doInitializeDefaultPreferences();

            // 3. 启动核心服务
            ruyiCore = new RuyiCore(logger);
            ruyiCore.startBackgroundJobs();

            logger.logInfo("Ruyi activated successfully.");
        } catch (Exception e) {
            String msg = "Failed to activate Ruyi plugin";
            logger.logError(msg, e);
            throw new RuntimeException(msg, e);
        }

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // ruyiCore.shutdown();
        // plugin = null;
        // super.stop(context);
        try {
            // 1. 停止核心服务
            if (ruyiCore != null) {
                ruyiCore.shutdown();
            }
            // 2. 清理资源
            cleanupResources();

            logger.logInfo("Ruyi plugin deactivated");
        } catch (Exception e) {
            logger.logError("Error during plugin deactivation", e);
        } finally {
            plugin = null;
            super.stop(context);
        }
    }

    public static Activator getDefault() {
        return plugin;
    }

    public RuyiCore getRuyiCore() {
        return ruyiCore;
    }

    public RuyiLogger getLogger() {
        return logger;
    }

    @Override
    public IPreferenceStore getPreferenceStore() {
        return super.getPreferenceStore();
    }

    private void cleanupResources() {
        // 清理打开的文件句柄等资源
    }

    /**
     * 创建错误状态对象
     */
    public static IStatus createError(String message, Throwable ex) {
        return new Status(IStatus.ERROR, PLUGIN_ID, message, ex);
    }

    // public static void initializeImageRegistry() {
    // // 示例图片注册 (实际路径需要调整)
    // Activator.getDefault().getImageRegistry().put("folder",
    // imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/folder.png"));
    // Activator.getDefault().getImageRegistry().put("version",
    // imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/version.png"));
    // // 其他图标...
    // }
    // @Override
    // protected void initializeImageRegistry(ImageRegistry reg) {
    // registerImage(reg, "ruyi_logo", "icons/ruyi_64.png");
    // registerImage(reg, "install", "icons/wizard/install.png");
    // registerImage(reg, "upgrade", "icons/wizard/upgrade.png");
    // // 更多图标...
    // }
}
