package org.ruyisdk.ruyi.services;

/**
 * RuyiApi专用异常.
 */
public class RuyiApiException extends Exception {
    /**
     * 构造异常.
     *
     * @param message 异常消息
     */
    public RuyiApiException(String message) {
        super(message);
    }

    /**
     * 构造异常.
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public RuyiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
