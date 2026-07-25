package com.flab.orderplatform.order.domain.event;

import com.flab.orderplatform.order.domain.DomainEvent;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent extends DomainEvent {
    private static final String TOPIC = "MSG-ORDER-CREATED";

    private final Long buyerId;
    private final List<OrderItemDto> orderItems;
    private final Long totalAmount;

    @Builder
    protected OrderCreatedEvent(String aggregateId,
                                LocalDateTime occurredOn,
                                Long buyerId,
                                List<OrderItemDto> orderItems,
                                Long totalAmount) {
        super(aggregateId, occurredOn);
        this.buyerId = buyerId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getAction() {
        return "OrderCreated";
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }

    public class OrderItemDto {
        private final Long productId;
        private final Integer quantity;
        private final Long unitPrice;

        @Builder
        public OrderItemDto(Long productId,
                            Integer quantity,
                            Long unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }
}
