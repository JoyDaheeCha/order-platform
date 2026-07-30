package com.flab.orderplatform.order.domain.event;

import com.flab.orderplatform.order.domain.DomainEvent;
import com.flab.orderplatform.shared.event.EventContract;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderCreatedEvent extends DomainEvent {

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
    public EventContract toPayload() {
        return new OrderCreatedPayload(
                getAggregateId(),
                buyerId,
                orderItems.stream()
                        .map(i -> new OrderCreatedPayload.OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                        .toList(),
                totalAmount
        );
    }

    @Builder
    public record OrderItemDto(
            Long productId,
            Integer quantity,
            Long unitPrice
    ) {
    }
}
