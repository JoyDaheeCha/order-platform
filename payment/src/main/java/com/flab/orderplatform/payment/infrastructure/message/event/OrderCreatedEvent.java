package com.flab.orderplatform.payment.infrastructure.message.event;

import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.domain.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// TODO shared 분리 방법 찾기
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

    public PaymentCreateCommand toCommand() {
        return PaymentCreateCommand.builder()
                .orderNumber(this.getAggregateId())
                .buyerId(buyerId)
                .amount(totalAmount)
                .build();
    }

    @Builder
    public record OrderItemDto(
            Long productId,
            Integer quantity,
            Long unitPrice
    ) {
    }
}
