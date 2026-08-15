package com.flab.orderplatform.inventory.application.command;

import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.Builder;

@Builder
public record InventoryDecreaseCommand(
        String orderNumber,
        ProductDto product
) {
    public Inventory decreaseStock(Inventory inventory) {
        return inventory.decreaseStock(orderNumber, product.quantityToDecrease);
    }

    @Builder
    public record ProductDto(
            String productCode,
            Integer quantityToDecrease
    ){
    }

}
