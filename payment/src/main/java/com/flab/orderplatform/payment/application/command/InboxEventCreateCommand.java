package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.InboxEvent;
import lombok.Builder;

@Builder
public record InboxEventCreateCommand(
        String eventId,
        String eventType,
        String aggregateType,
        String payload
) {
    public InboxEvent create() {
        return InboxEvent.create(eventId, aggregateType, eventType, payload);
    }
}
