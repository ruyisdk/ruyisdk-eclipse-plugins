package org.ruyisdk.core.ruyi.model;

/**
 * Encapsulates the result of a Ruyi environment check operation.
 *
 * <p>This class represents the outcome of checking the Ruyi installation status,
 * including whether installation/upgrade is needed and version information.
 */
public class CheckResult {
    /**
     * Enumeration of possible actions required after environment check.
     */
    public enum ActionType {
        /** Ruyi needs to be installed (not currently installed). */
        INSTALL,
        
        /** Ruyi needs to be upgraded (newer version available). */
        UPGRADE,
        
        /** No action needed (Ruyi is up-to-date). */
        NOTHING
    }

    private final ActionType action;
    private final String message;
    private final RuyiVersion currentVersion;
    private final RuyiVersion latestVersion;

    /**
     * Constructs a new CheckResult instance.
     *
     * @param action the required action type
     * @param message descriptive message about the check result
     * @param current the currently installed version (may be null)
     * @param latest the latest available version (may be null)
     */
    private CheckResult(ActionType action, String message, RuyiVersion current, RuyiVersion latest) {
        this.action = action;
        this.message = message;
        this.currentVersion = current;
        this.latestVersion = latest;
    }

    /**
     * Creates a result indicating Ruyi needs to be installed.
     *
     * @param msg description of why installation is needed
     * @return new CheckResult instance with INSTALL action
     */
    public static CheckResult needInstall(String msg) {
        return new CheckResult(ActionType.INSTALL, msg, null, null);
    }

    /**
     * Creates a result indicating Ruyi needs to be upgraded.
     *
     * @param current currently installed version
     * @param latest latest available version
     * @param msg description of why upgrade is needed
     * @return new CheckResult instance with UPGRADE action
     */
    public static CheckResult needUpgrade(RuyiVersion current, RuyiVersion latest, String msg) {
        return new CheckResult(ActionType.UPGRADE, msg, current, latest);
    }

    /**
     * Creates a result indicating no action is needed.
     *
     * @return new CheckResult instance with NOTHING action
     */
    public static CheckResult ok() {
        return new CheckResult(ActionType.NOTHING, "Ruyi is up-to-date", null, null);
    }

    // Getters

    /**
     * Gets the required action type.
     *
     * @return the action type
     */
    public ActionType getAction() {
        return action;
    }

    /**
     * Gets the descriptive message about the check result.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the currently installed version.
     *
     * @return current version (may be null)
     */
    public RuyiVersion getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Gets the latest available version.
     *
     * @return latest version (may be null)
     */
    public RuyiVersion getLatestVersion() {
        return latestVersion;
    }

    /**
     * Checks if any action is required.
     *
     * @return true if action is needed (install or upgrade), false otherwise
     */
    public boolean needAction() {
        return action != ActionType.NOTHING;
    }
}