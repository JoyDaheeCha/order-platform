package com.flab.orderplatform.order.application.exception;

import java.util.Collection;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Collection<String> productCodes) {
        super(String.format("존재하지 않는 상품코드(%s)가 등록되어 있습니다.", productCodes));
    }
}
