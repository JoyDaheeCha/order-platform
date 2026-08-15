package com.flab.orderplatform.inventory.application.exception;

import com.flab.orderplatform.inventory.common.BusinessErrorCode;
import com.flab.orderplatform.inventory.common.BusinessException;

import java.util.List;

public class InventoryNotFoundException extends BusinessException {
    public InventoryNotFoundException(List<String> notRegisteredProductCodes) {
        super("재고에 존재하지 않는 상품입니다. (상품코드: %s)".formatted(notRegisteredProductCodes));
    }

    public InventoryNotFoundException(String notRegisteredProductCode) {
        this(List.of(notRegisteredProductCode));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return BusinessErrorCode.NOT_FOUND;
    }
}
