package com.flab.orderplatform.inventory.application.command;

import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.Builder;

@Builder
public record InventoryDecreaseCommand(
        String orderNumber,
        ProductDto product
) {
    public Inventory decreaseInventory(Inventory inventory) {
        return inventory.decreaseInventory(orderNumber, product.quantityToDecrease);
    }

    @Builder
    public record ProductDto(
            String productCode,
            Integer quantityToDecrease
    ){
    }

}
