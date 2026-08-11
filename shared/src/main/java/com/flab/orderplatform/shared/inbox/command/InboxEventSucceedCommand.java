package com.flab.orderplatform.shared.inbox.command;

import com.flab.orderplatform.shared.inbox.InboxEvent;
import lombok.Builder;

@Builder
public record InboxEventSucceedCommand(
        String eventId
) {
    public InboxEvent succeed(InboxEvent event) {
        return event.succeed();
    }
}
