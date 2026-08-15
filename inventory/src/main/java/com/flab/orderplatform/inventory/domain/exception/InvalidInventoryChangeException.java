package com.flab.orderplatform.inventory.domain.exception;

import com.flab.orderplatform.inventory.common.BusinessErrorCode;
import com.flab.orderplatform.inventory.common.BusinessException;

public class InvalidInventoryChangeException extends BusinessException {
    public InvalidInventoryChangeException(String message) {
        super(message);
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return BusinessErrorCode.INVALID_REQUEST;
    }
}
