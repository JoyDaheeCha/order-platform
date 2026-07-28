package com.flab.orderplatform.payment.common.exception;

import com.flab.orderplatform.payment.common.BusinessException;
import com.flab.orderplatform.payment.common.common.BusinessErrorCode;

import static com.flab.orderplatform.payment.common.common.BusinessErrorCode.NOT_FOUND;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException(String orderNumber) {
        super(String.format("입력된 주문정보(%s)에 대해 존재하는 결제 정보가 없습니다.", orderNumber));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return NOT_FOUND;
    }
}
