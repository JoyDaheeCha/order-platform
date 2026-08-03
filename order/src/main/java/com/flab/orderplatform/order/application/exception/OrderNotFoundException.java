package com.flab.orderplatform.order.application.exception;

import com.flab.orderplatform.order.common.BusinessErrorCode;
import com.flab.orderplatform.order.common.exception.BusinessException;

import static com.flab.orderplatform.order.common.BusinessErrorCode.NOT_FOUND;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String orderNumber) {
        super(String.format("존재하지 않는 주문(주문번호:%s)입니다.", orderNumber));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return NOT_FOUND;
    }
}
