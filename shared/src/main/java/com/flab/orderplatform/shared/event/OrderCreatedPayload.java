package com.flab.orderplatform.shared.event;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED_TOPIC;

/**
 * 주문이 생성되었다 이벤트 페이로드
 */
public record OrderCreatedPayload(
        String orderNumber,
        List<OrderItem> orderItems
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
            String productCode,
            Integer quantity
    ) {
    }
}
