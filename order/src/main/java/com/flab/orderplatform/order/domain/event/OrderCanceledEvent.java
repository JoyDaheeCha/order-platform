package com.flab.orderplatform.order.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.*;

/**
 * 주문이 취소되었다 이벤트
 */
@Getter
public class OrderCanceledEvent extends OrderOutboxEvent {

    private final String orderNumber;
    private final Long totalAmount;
    private final List<OrderItemDto> orderItems;

    @Builder
    protected OrderCanceledEvent(String aggregateId,
                                 LocalDateTime occurredOn,
                                 String orderNumber,
                                 List<OrderItemDto> orderItems,
                                 Long totalAmount) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
    }


    @Override
    public String getAction() {
        return ORDER_CANCELLED;
    }

    @Override
    public String getTopic() {
        return ORDER_CANCELLED_TOPIC;
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
