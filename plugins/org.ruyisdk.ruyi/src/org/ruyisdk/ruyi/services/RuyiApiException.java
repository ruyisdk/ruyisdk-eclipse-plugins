package org.ruyisdk.ruyi.services;

import java.io.IOException;
import org.json.JSONException;
import org.ruyisdk.core.exception.PluginException;

/**
 * RuyiApi专用异常.
 */
public class RuyiApiException extends PluginException {
    private static final long serialVersionUID = 1L;

    /**
     * 构造异常.
     *
     * @param message 异常消息
     */
    private RuyiApiException(String message) {
        super(message);
    }

    /**
     * 构造异常.
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    private RuyiApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Operation was cancelled. */
    public static RuyiApiException cancelled() {
        return new RuyiApiException("ruyi command was cancelled");
    }

    /** Unexpected status code. */
    public static RuyiApiException unexpectedStatusCode(int code) {
        return new RuyiApiException("Unexpected status code: " + code);
    }

    /** Invalid argument provided to CLI. */
    public static RuyiApiException invalidArgument(String message) {
        return new RuyiApiException(message);
    }

    /** I/O error during API request. */
    public static RuyiApiException ioError(IOException cause) {
        return new RuyiApiException("I/O error during API request", cause);
    }

    /** JSON error during API request. */
    public static RuyiApiException jsonError(JSONException cause) {
        return new RuyiApiException("JSON error during API request", cause);
    }
}
