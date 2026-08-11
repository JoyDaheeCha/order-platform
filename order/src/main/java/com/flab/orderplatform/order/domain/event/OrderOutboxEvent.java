package com.flab.orderplatform.order.domain.event;

import com.flab.orderplatform.shared.domain.DomainEvent;

import java.time.LocalDateTime;

public abstract class OrderOutboxEvent extends DomainEvent {
    protected OrderOutboxEvent(String aggregateId, LocalDateTime occurredOn) {
        super(aggregateId, occurredOn);
    }
}