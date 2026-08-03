package com.flab.orderplatform.inventory.application.port.out;

public interface InventoryHistoryRepository {
    boolean existsByOrderNumber(String orderNumber);
}
