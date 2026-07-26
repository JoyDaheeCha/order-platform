package com.flab.orderplatform.order.common.exception;

import com.flab.orderplatform.order.common.BusinessErrorCode;

public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public abstract BusinessErrorCode getErrorCode();
}
