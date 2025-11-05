package com.deepreach.common.exception;

/**
 * 余额不足异常
 */
public class BalanceNotEnoughException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BalanceNotEnoughException(String message) {
        super(message);
    }

    public BalanceNotEnoughException(String message, Throwable cause) {
        super(message, cause);
    }
}

