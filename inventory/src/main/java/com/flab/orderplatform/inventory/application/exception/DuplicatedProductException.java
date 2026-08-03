package com.flab.orderplatform.inventory.application.exception;


import com.flab.orderplatform.inventory.common.BusinessErrorCode;
import com.flab.orderplatform.inventory.common.BusinessException;

import java.util.Collection;

import static com.flab.orderplatform.inventory.common.BusinessErrorCode.INVALID_REQUEST;


public class DuplicatedProductException extends BusinessException {
    public DuplicatedProductException(Collection<String> productCodes) {
        super("재고 조정시, 상품을 중복하여 요청할 수 없습니다. (상품코드: %s)"
                .formatted(String.join(",", productCodes)));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return INVALID_REQUEST;
    }
}
