package com.flab.orderplatform.order.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Getter
public class OrderPaymentPreparedEvent extends OrderOutboxEvent {

    private final String orderNumber;
    private final Long buyerId;
    private final Long amount;

    @Builder
    protected OrderPaymentPreparedEvent(String aggregateId,
                                        LocalDateTime occurredOn,
                                        String orderNumber,
                                        Long buyerId,
                                        Long amount) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
        this.buyerId = buyerId;
        this.amount = amount;
    }


    @Override
    public String getAction() {
        return ORDER_PAYMENT_PREPARED;
    }

    @Override
    public String getTopic() {
        return ORDER_PAYMENT_PREPARED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_ORDER;
    }
}
