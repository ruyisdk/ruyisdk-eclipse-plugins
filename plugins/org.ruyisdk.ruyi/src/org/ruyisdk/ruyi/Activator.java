package org.ruyisdk.ruyi;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.ruyisdk.ruyi.core.RuyiCore;
import org.ruyisdk.ruyi.util.RuyiLogger;

/**
 * Activator for the Ruyi plugin.
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.ruyisdk.ruyi";
    private static Activator plugin; // 共享实例
    private RuyiCore ruyiCore; // 核心服务

    private static final RuyiLogger LOGGER =
                    new RuyiLogger(Platform.getLog(FrameworkUtil.getBundle(Activator.class)), PLUGIN_ID);

    /**
     * Starts the plugin.
     *
     * @param context bundle context
     * @throws Exception if start fails
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // 2. 加载默认首选项
        // RuyiPreferenceInitializer.doInitializeDefaultPreferences();

        // 3. 启动核心服务
        ruyiCore = new RuyiCore(LOGGER);
        ruyiCore.startBackgroundJobs();

        LOGGER.logInfo("Ruyi activated successfully.");
    }

    /**
     * Stops the plugin.
     *
     * @param context bundle context
     * @throws Exception if stop fails
     */
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

            LOGGER.logInfo("Ruyi plugin deactivated");
        } catch (Exception e) {
            LOGGER.logError("Error during plugin deactivation", e);
        } finally {
            plugin = null;
            super.stop(context);
        }
    }

    /**
     * Gets the shared plugin instance.
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Gets the Ruyi core service.
     *
     * @return the core service
     */
    public RuyiCore getRuyiCore() {
        return ruyiCore;
    }

    /**
     * Returns a logger that does not depend on {@link #getDefault()} being initialized.
     *
     * <p>
     * This is safe to call during early class loading (e.g., before {@link #start(BundleContext)}).
     */
    public static RuyiLogger getLogger() {
        return LOGGER;
    }

    @Override
    public IPreferenceStore getPreferenceStore() {
        return super.getPreferenceStore();
    }

    private void cleanupResources() {
        // 清理打开的文件句柄等资源
    }

    /**
     * Creates an error status.
     *
     * @param message error message
     * @param ex exception
     * @return error status
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
