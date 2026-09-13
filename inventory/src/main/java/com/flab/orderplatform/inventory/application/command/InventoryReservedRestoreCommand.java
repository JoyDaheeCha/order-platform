package com.flab.orderplatform.inventory.application.command;

import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.Builder;

@Builder
public record InventoryReservedRestoreCommand(
        String orderNumber,
        ProductDto product
) {
    public Inventory restore(Inventory inventory) {
        return inventory.restoreReservedInventory(orderNumber, product.quantity);
    }

    @Builder
    public record ProductDto(
            String productCode,
            Integer quantity
    ){
    }

}
