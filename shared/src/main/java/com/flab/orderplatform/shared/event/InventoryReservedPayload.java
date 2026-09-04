package com.flab.orderplatform.shared.event;

import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_RESERVED;
import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_RESERVED_TOPIC;

public record InventoryReservedPayload(
        String orderNumber
) implements EventContract{
    @Override
    public String eventType() {
        return INVENTORY_RESERVED;
    }

    @Override
    public String topic() {
        return INVENTORY_RESERVED_TOPIC;
    }
}
