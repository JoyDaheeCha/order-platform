package com.flab.orderplatform.order.domain.event;

import com.flab.orderplatform.order.domain.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
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

    public record OrderItemDto(Long productId, Integer quantity, Long unitPrice) {
        @Builder
        public OrderItemDto {
        }
        }
}
