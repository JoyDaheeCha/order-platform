package com.flab.orderplatform.payment.domain;

import com.flab.orderplatform.shared.domain.DomainEvent;

import java.time.LocalDateTime;

public abstract class PaymentOutboxEvent extends DomainEvent {
    protected PaymentOutboxEvent(String aggregateId, LocalDateTime occurredOn) {
        super(aggregateId, occurredOn);
    }
}