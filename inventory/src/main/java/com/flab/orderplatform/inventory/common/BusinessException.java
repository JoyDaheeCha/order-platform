package com.flab.orderplatform.inventory.common;

public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public abstract BusinessErrorCode getErrorCode();
}
