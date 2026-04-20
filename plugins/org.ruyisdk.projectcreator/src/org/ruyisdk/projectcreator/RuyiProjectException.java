package org.ruyisdk.projectcreator;

import org.ruyisdk.core.exception.PluginException;

/**
 * Exception thrown when project creation or build operations fail.
 */
public class RuyiProjectException extends PluginException {
    private static final long serialVersionUID = 1L;

    private RuyiProjectException(String message) {
        super(message);
    }

    private RuyiProjectException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Eclipse project creation failed. */
    public static RuyiProjectException creationFailed(String projectName, Throwable cause) {
        return new RuyiProjectException("Failed to create project: " + projectName, cause);
    }

    /** Project template not found in bundle. */
    public static RuyiProjectException templateNotFound(String templatePath) {
        return new RuyiProjectException("Template not found: " + templatePath);
    }

    /** Failed to create a folder inside the project. */
    public static RuyiProjectException folderCreationFailed(String path, Throwable cause) {
        return new RuyiProjectException("Failed to create folder: " + path, cause);
    }

    /** Failed to read a template file. */
    public static RuyiProjectException fileReadFailed(String file, Throwable cause) {
        return new RuyiProjectException("Failed to read template: " + file, cause);
    }

    /** Failed to write a file into the project. */
    public static RuyiProjectException fileWriteFailed(String file, Throwable cause) {
        return new RuyiProjectException("Failed to write file: " + file, cause);
    }

    /** Failed to copy a file into the project. */
    public static RuyiProjectException fileCopyFailed(String file, Throwable cause) {
        return new RuyiProjectException("Failed to copy file: " + file, cause);
    }

    /** Failed to read a persistent project property. */
    public static RuyiProjectException propertyAccessFailed(String property, Throwable cause) {
        return new RuyiProjectException("Failed to retrieve " + property + " from project properties", cause);
    }

    /** A required persistent project property is not set. */
    public static RuyiProjectException propertyMissing(String property) {
        return new RuyiProjectException(property + " not set for project");
    }
}
