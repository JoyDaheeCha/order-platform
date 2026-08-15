package com.flab.orderplatform.shared.event;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_PAID;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_PAID_TOPIC;

public record OrderPaidPayload(
        String orderNumber,
        List<OrderItemDto> orderItems
) implements EventContract {

    @Override
    public String eventType() {
        return ORDER_PAID;
    }

    @Override
    public String topic() {
        return ORDER_PAID_TOPIC;
    }

    public record OrderItemDto(
            String productCode,
            Integer quantity
    ) {
    }
}
