package com.flab.orderplatform.inventory.application.exception;

import com.flab.orderplatform.inventory.common.BusinessErrorCode;
import com.flab.orderplatform.inventory.common.BusinessException;

import static com.flab.orderplatform.inventory.common.BusinessErrorCode.CONFLICT_REQUEST;

public class InventoryDecreasementFailureByConcurrencyException extends BusinessException {
    public InventoryDecreasementFailureByConcurrencyException(String orderNumber) {
        super(String.format("동시성 충돌로 인해 재고 감소에 실패했습니다. (주문번호:%s)", orderNumber));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return CONFLICT_REQUEST;
    }
}
