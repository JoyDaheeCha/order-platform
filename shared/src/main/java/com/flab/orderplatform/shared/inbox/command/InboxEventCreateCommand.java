package com.flab.orderplatform.shared.inbox.command;

import com.flab.orderplatform.shared.inbox.InboxEvent;
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
