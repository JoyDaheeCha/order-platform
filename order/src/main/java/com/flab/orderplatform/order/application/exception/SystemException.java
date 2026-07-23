package com.flab.orderplatform.order.application.exception;

/**
 * 시스템 or 기술 예외
 */
public class SystemException extends RuntimeException {

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
