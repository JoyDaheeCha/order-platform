package com.flab.orderplatform.inventory.domain.type;

import lombok.Getter;

public enum InventoryUpdateRequestType {
    RESERVE("재고 선점"),
    DECREASE("가용 재고 차감"),
    RESTORE_RESERVATION("재고 선점 취소"),
    RESTORE_INVENTORY("가용 재고 원복");

    @Getter
    final String description;

    InventoryUpdateRequestType(String description) {
        this.description = description;
    }
}
