package com.flab.orderplatform.shared.event;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED_TOPIC;

// TODO: order - inventory 간 통신 규약에 맞게 내부 필드 최소화
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
            String productCode, // TODO productCode 를 기존 다른 이벤트 필드에도 추가
            Integer quantity,
            Long unitPrice
    ) {
    }
}
