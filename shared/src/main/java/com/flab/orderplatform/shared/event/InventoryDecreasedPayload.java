package com.flab.orderplatform.shared.event;

import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_DECREASED;
import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_DECREASED_TOPIC;

public record InventoryDecreasedPayload(
        String orderNumber
) implements EventContract {

    @Override
    public String eventType() {
        return INVENTORY_DECREASED;
    }

    @Override
    public String topic() {
        return INVENTORY_DECREASED_TOPIC;
    }

    public record OrderItemDto(
            String productCode,
            Integer quantity
    ) {
    }
}
