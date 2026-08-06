package com.flab.orderplatform.shared.event;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED_TOPIC;

public record OrderCreatedPayload(
        String orderNumber,
        Long buyerId,
        List<OrderItem> orderItems,
        Long totalAmount
) implements EventContract {

    @Override
    public String eventType() {
        return ORDER_CREATED;
    }

    @Override
    public String topic() {
        return ORDER_CREATED_TOPIC;
    }

    public record OrderItem(
            Long productId,
            Integer quantity,
            Long unitPrice
    ) {
    }
}
