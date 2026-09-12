package com.flab.orderplatform.shared.event;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CANCELLED;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CANCELLED_TOPIC;

/**
 * 주문이 취소되었다 이벤트 페이로드
 */
public record OrderCanceledPayload(
        String orderNumber,
        List<OrderItem> orderItems,
        Long totalAmount // TODO AMOUNT 필요 x
) implements EventContract {

    @Override
    public String eventType() {
        return ORDER_CANCELLED;
    }

    @Override
    public String topic() {
        return ORDER_CANCELLED_TOPIC;
    }

    public record OrderItem(
            String productCode,
            Integer quantity
    ) {
    }
}
