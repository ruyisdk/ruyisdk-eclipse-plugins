package org.ruyisdk.ruyi.core;

import org.eclipse.ui.IStartup;
import org.ruyisdk.ruyi.Activator;

/**
 * Eclipse启动时自动执行的检测逻辑
 */
public class RuyiStartup implements IStartup {
    @Override
    public void earlyStartup() {
        // 延迟启动检测以避免影响IDE启动性能
        Activator.getDefault().getRuyiCore().startBackgroundCheck();
    }
}
