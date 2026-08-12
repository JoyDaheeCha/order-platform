package com.flab.orderplatform.inventory.domain.event;

import com.flab.orderplatform.shared.domain.DomainEvent;

import java.time.LocalDateTime;

public abstract class InventoryOutboxEvent extends DomainEvent {
    protected InventoryOutboxEvent(String aggregateId, LocalDateTime occurredOn) {
        super(aggregateId, occurredOn);
    }
}