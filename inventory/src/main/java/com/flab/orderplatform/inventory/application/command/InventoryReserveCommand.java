package com.flab.orderplatform.inventory.application.command;

import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.Builder;

@Builder
public record InventoryReserveCommand(
        String orderNumber,
        ProductDto product
) {
    public Inventory reserve(Inventory inventory) {
        return inventory.reserveInventory(orderNumber, product.quantity);
    }

    @Builder
    public record ProductDto(
            String productCode,
            Integer quantity
    ){
    }

}
