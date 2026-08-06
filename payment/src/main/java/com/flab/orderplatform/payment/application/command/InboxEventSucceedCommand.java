package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.InboxEvent;
import lombok.Builder;

@Builder
public record InboxEventSucceedCommand(
        String eventId
) {
    public InboxEvent succeed(InboxEvent event) {
        return event.succeed();
    }
}
