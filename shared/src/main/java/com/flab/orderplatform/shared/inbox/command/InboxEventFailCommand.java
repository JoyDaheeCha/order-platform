package com.flab.orderplatform.shared.inbox.command;

import com.flab.orderplatform.shared.inbox.InboxEvent;
import lombok.Builder;

@Builder
public record InboxEventFailCommand(
        String eventId
) {
    public InboxEvent fail(InboxEvent event) {
        return event.fail();
    }
}
