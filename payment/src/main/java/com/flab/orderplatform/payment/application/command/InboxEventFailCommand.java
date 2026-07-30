package com.flab.orderplatform.payment.application.command;

import com.flab.orderplatform.payment.domain.InboxEvent;
import lombok.Builder;

@Builder
public record InboxEventFailCommand(
        String eventId
) {
    public InboxEvent fail(InboxEvent event) {
        return event.fail();
    }
}
