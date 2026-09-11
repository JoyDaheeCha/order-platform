package com.flab.orderplatform.shared.event;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_FAILED;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_FAILED_TOPIC;

public record OrderFailedPayload(
        String orderNumber,
        List<OrderItemDto> orderItems
) implements EventContract {

    @Override
    public String eventType() {
        return ORDER_FAILED;
    }

    @Override
    public String topic() {
        return ORDER_FAILED_TOPIC;
    }

    public record OrderItemDto(
            String productCode,
            Integer quantity
    ) {
    }
}
