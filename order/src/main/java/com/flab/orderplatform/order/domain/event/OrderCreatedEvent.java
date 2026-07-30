package com.flab.orderplatform.order.domain.event;

import com.flab.orderplatform.order.domain.DomainEvent;
import com.flab.orderplatform.shared.event.EventMeta;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderCreatedEvent extends DomainEvent {

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
    public EventMeta toPayload() {
        return new OrderCreatedPayload(
                orderNumber,
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
