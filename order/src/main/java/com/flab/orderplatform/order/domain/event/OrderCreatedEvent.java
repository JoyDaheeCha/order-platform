package com.flab.orderplatform.order.domain.event;

import com.flab.orderplatform.order.domain.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED_TOPIC;

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
    public String getAction() {
        return "OrderCreated";
    }

    @Override
    public String getTopic() {
        return ORDER_CREATED_TOPIC;
    }

    @Builder
    public record OrderItemDto(
            Long productId,
            Integer quantity,
            Long unitPrice
    ) {
    }
}
