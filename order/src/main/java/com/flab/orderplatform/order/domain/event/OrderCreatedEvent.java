package com.flab.orderplatform.order.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.*;

/**
 * 주문이 생성되었다 이벤트
 */
@Getter
public class OrderCreatedEvent extends OrderOutboxEvent {

    private final String orderNumber;
    private final List<OrderItemDto> orderItems;

    @Builder
    protected OrderCreatedEvent(String aggregateId,
                                LocalDateTime occurredOn,
                                String orderNumber,
                                List<OrderItemDto> orderItems) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
        this.orderItems = orderItems;
    }


    @Override
    public String getAction() {
        return ORDER_CREATED;
    }

    @Override
    public String getTopic() {
        return ORDER_CREATED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_ORDER;
    }

    @Builder
    public record OrderItemDto(
            String productCode,
            Integer quantity
    ) {
    }
}
