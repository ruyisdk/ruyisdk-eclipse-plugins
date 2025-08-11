package org.ruyisdk.core.ruyi.model;

/**
 * 封装Ruyi环境检测结果
 */
public class CheckResult {
    public enum ActionType {
        INSTALL, // 需要安装
        UPGRADE, // 需要升级
        NOTHING // 无需操作
    }

    private final ActionType action;
    private final String message;
    private final RuyiVersion currentVersion;
    private final RuyiVersion latestVersion;

    private CheckResult(ActionType action, String message, RuyiVersion current, RuyiVersion latest) {
        this.action = action;
        this.message = message;
        this.currentVersion = current;
        this.latestVersion = latest;
    }

    public static CheckResult needInstall(String msg) {
        return new CheckResult(ActionType.INSTALL, msg, null, null);
    }

    public static CheckResult needUpgrade(RuyiVersion current, RuyiVersion latest, String msg) {
        return new CheckResult(ActionType.UPGRADE, msg, current, latest);
    }

    public static CheckResult ok() {
        return new CheckResult(ActionType.NOTHING, "Ruyi is up-to-date", null, null);
    }

    // Getters
    public ActionType getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public RuyiVersion getCurrentVersion() {
        return currentVersion;
    }

    public RuyiVersion getLatestVersion() {
        return latestVersion;
    }

    public boolean needAction() {
        return action != ActionType.NOTHING;
    }
}
