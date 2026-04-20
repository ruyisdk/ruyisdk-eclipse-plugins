package org.ruyisdk.core.exception;

/**
 * Base exception for all Ruyi SDK domain exceptions.
 *
 * <p>
 * Subclasses represent specific failure categories (CLI errors, API errors, etc.). UI code can
 * catch this base type to handle all domain errors uniformly, or catch a specific subclass when
 * different handling is required.
 * </p>
 *
 * <p>
 * Subclasses should declare private constructors and expose public static factory methods rather
 * than being instantiated with new directly.
 * </p>
 */
public class PluginException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SDK exception with the specified message.
     *
     * @param message the detail message
     */
    protected PluginException(String message) {
        super(message);
    }

    /**
     * Constructs a new SDK exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    protected PluginException(String message, Throwable cause) {
        super(makeMessage(message, cause), cause);
    }

    private static String makeMessage(String message, Throwable cause) {
        if (cause == null) {
            return message;
        }
        final var exType = cause.getClass().getSimpleName();
        final var msg = message + "\n" + exType;

        final var exMsg = cause.getMessage();
        if (exMsg != null) {
            return msg + ": " + exMsg;
        }
        return msg;
    }
}
