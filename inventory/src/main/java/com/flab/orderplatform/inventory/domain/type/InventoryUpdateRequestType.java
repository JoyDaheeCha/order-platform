package com.flab.orderplatform.inventory.domain.type;

public enum InventoryUpdateRequestType {
    RESERVE("재고 선점"),
    DECREASE("가용 재고 차감");

    final String description;

    InventoryUpdateRequestType(String description) {
        this.description = description;
    }
}
