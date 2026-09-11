package com.flab.orderplatform.payment.domain.event;

import com.flab.orderplatform.payment.domain.PaymentOutboxEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Getter
public class PaymentFailedEvent extends PaymentOutboxEvent {

    private final String orderNumber;

    @Builder
    protected PaymentFailedEvent(String aggregateId,
                                 LocalDateTime occurredOn,
                                 String orderNumber) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
    }

    @Override
    public String getAction() {
        return PAYMENT_FAILED;
    }

    @Override
    public String getTopic() {
        return PAYMENT_FAILED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_PAYMENT;
    }
}
