package com.flab.orderplatform.inventory.application.port.out;

import com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType;

public interface InventoryHistoryRepository {
    boolean existsByOrderNumber(String orderNumber, InventoryUpdateRequestType requestType);
}
