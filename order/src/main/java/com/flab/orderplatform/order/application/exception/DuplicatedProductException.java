package com.flab.orderplatform.order.application.exception;


import com.flab.orderplatform.order.common.BusinessErrorCode;
import com.flab.orderplatform.order.common.exception.BusinessException;

import java.util.Collection;

import static com.flab.orderplatform.order.common.BusinessErrorCode.INVALID_REQUEST;


public class DuplicatedProductException extends BusinessException {
    public DuplicatedProductException(Collection<String> productCodes) {
        super("동일한 상품 코드가 주문에서 중복되어 존재합니다. (상품코드: %s)"
                .formatted(String.join(",", productCodes)));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return INVALID_REQUEST;
    }
}
