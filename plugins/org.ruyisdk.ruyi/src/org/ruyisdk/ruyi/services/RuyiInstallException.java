package org.ruyisdk.ruyi.services;

import java.io.IOException;
import org.ruyisdk.core.exception.PluginException;

/**
 * Exception thrown when Ruyi package manager installation fails.
 */
public class RuyiInstallException extends PluginException {
    private static final long serialVersionUID = 1L;

    private RuyiInstallException(String message) {
        super(message);
    }

    private RuyiInstallException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Failed to create the installation directory. */
    public static RuyiInstallException directoryCreationFailed(String path, IOException cause) {
        return new RuyiInstallException("Failed to create installation directory: " + path, cause);
    }

    /** Filesystem I/O error during installation preparation. */
    public static RuyiInstallException filesystemError(String message, IOException cause) {
        return new RuyiInstallException(message, cause);
    }

    /** Available disk space is below the required threshold. */
    public static RuyiInstallException insufficientDiskSpace(String requiredMb,
            String availableMb) {
        return new RuyiInstallException(
                String.format("Insufficient disk space. Required: %s MB, available: %s MB",
                        requiredMb, availableMb));
    }

    /** All download attempts for the Ruyi binary failed. */
    public static RuyiInstallException downloadFailed(String message, Throwable cause) {
        return new RuyiInstallException(message, cause);
    }

    /** Downloaded executable not found at expected path. */
    public static RuyiInstallException fileNotFound(String path) {
        return new RuyiInstallException("File not found: " + path);
    }

    /** Failed to set executable permissions on the Ruyi binary. */
    public static RuyiInstallException permissionFailed(String path, Throwable cause) {
        return new RuyiInstallException("Failed to set executable permissions: " + path, cause);
    }

    /** Executable permissions could not be verified after setting them. */
    public static RuyiInstallException permissionVerifyFailed(String path) {
        return new RuyiInstallException("Failed to set executable permissions for: " + path
                + ". You may need root/sudo privileges.");
    }

    /** Post-installation validation failed. */
    public static RuyiInstallException validationFailed(String message) {
        return new RuyiInstallException(message);
    }
}
