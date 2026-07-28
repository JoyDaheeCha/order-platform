package com.flab.orderplatform.payment.common;

import com.flab.orderplatform.payment.common.common.BusinessErrorCode;

public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public abstract BusinessErrorCode getErrorCode();
}
