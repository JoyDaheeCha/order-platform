package com.flab.orderplatform.inventory.domain.exception;

import com.flab.orderplatform.inventory.common.BusinessErrorCode;
import com.flab.orderplatform.inventory.common.BusinessException;

public class ReservedInventoryShortageException extends BusinessException {
    public ReservedInventoryShortageException(Integer currentStockQuantity, Integer requiredStockQuantity) {
        super("선점된 재고 수량이 부족합니다. 현재 재고 선점 수량: %d, 요청된 상품 수량: %d".formatted(currentStockQuantity, requiredStockQuantity));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return BusinessErrorCode.INVALID_REQUEST;
    }
}
