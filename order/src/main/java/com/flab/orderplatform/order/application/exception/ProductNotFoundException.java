package com.flab.orderplatform.order.application.exception;

import com.flab.orderplatform.order.common.BusinessErrorCode;
import com.flab.orderplatform.order.common.exception.BusinessException;

import java.util.Collection;
import java.util.List;

import static com.flab.orderplatform.order.common.BusinessErrorCode.NOT_FOUND;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Collection<String> productCodes) {
        super(String.format("존재하지 않는 상품코드(%s)가 등록되어 있습니다.", String.join(",", productCodes)));
    }

    public ProductNotFoundException(String productCode) {
        this(List.of(productCode));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return NOT_FOUND;
    }
}
