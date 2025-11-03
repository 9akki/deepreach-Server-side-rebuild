package com.deepreach.common.exception;

/**
 * 营销实例数量不足异常
 *
 * @author DeepReach Team
 */
public class InsufficientMarketingInstanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientMarketingInstanceException(String message) {
        super(message);
    }

    public InsufficientMarketingInstanceException(String message, Throwable cause) {
        super(message, cause);
    }
}