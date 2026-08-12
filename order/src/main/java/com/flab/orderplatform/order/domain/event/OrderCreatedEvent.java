package com.flab.orderplatform.order.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Getter
public class OrderCreatedEvent extends OrderOutboxEvent {

    private final String orderNumber;
    private final Long buyerId;
    private final List<OrderItemDto> orderItems;
    private final Long totalAmount;

    @Builder
    protected OrderCreatedEvent(String aggregateId,
                                LocalDateTime occurredOn,
                                String orderNumber,
                                Long buyerId,
                                List<OrderItemDto> orderItems,
                                Long totalAmount) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
        this.buyerId = buyerId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
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
            Long productId,
            Integer quantity,
            Long unitPrice
    ) {
    }
}
