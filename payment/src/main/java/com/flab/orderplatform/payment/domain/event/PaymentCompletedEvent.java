package com.flab.orderplatform.payment.domain.event;

import com.flab.orderplatform.payment.domain.PaymentOutboxEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Getter
public class PaymentCompletedEvent extends PaymentOutboxEvent {

    private final String orderNumber;
    private final String pgTid;
    private final Long amount;

    @Builder
    protected PaymentCompletedEvent(String aggregateId,
                                    LocalDateTime occurredOn,
                                    String orderNumber,
                                    String pgTid,
                                    Long amount) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
        this.pgTid = pgTid;
        this.amount = amount;
    }

    @Override
    public String getAction() {
        return PAYMENT_COMPLETED;
    }

    @Override
    public String getTopic() {
        return PAYMENT_COMPLETED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_PAYMENT;
    }
}
