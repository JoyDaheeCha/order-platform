package com.flab.orderplatform.inventory.application.command;

import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.Builder;

/**
 * 가용 재고 원복 명령
 */
@Builder
public record InventoryRestoreCommand(
        String orderNumber,
        ProductDto product
) {
    public Inventory restore(Inventory inventory) {
        return inventory.restoreInventory(orderNumber, product.quantity);
    }

    @Builder
    public record ProductDto(
            String productCode,
            Integer quantity
    ){
    }

}
