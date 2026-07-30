package com.flab.orderplatform.payment.common;

public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public abstract BusinessErrorCode getErrorCode();
}
