package com.flab.orderplatform.inventory.application.exception;

import com.flab.orderplatform.inventory.common.BusinessErrorCode;
import com.flab.orderplatform.inventory.common.BusinessException;
import com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType;

import static com.flab.orderplatform.inventory.common.BusinessErrorCode.CONFLICT_REQUEST;

public class InventoryUpdateFailureByConcurrencyException extends BusinessException {
    public InventoryUpdateFailureByConcurrencyException(String orderNumber, InventoryUpdateRequestType requestType) {
        super(String.format("동시성 충돌로 인해 재고 수정에 실패했습니다. (주문번호:%s, 요청유형:%s)",
                orderNumber, requestType.getDescription()));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return CONFLICT_REQUEST;
    }
}
