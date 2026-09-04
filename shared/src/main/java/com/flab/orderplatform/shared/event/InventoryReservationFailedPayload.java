package com.flab.orderplatform.shared.event;

import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_RESERVATION_FAILED;
import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_RESERVATION_FAILED_TOPIC;

public record InventoryReservationFailedPayload(
        String orderNumber
) implements EventContract{
    @Override
    public String eventType() {
        return INVENTORY_RESERVATION_FAILED;
    }

    @Override
    public String topic() {
        return INVENTORY_RESERVATION_FAILED_TOPIC;
    }
}
