package com.flab.orderplatform.payment.infrastructure.message.inbox;

import com.flab.orderplatform.payment.domain.InboxEvent;

public interface InboxEventProcessor {
    String supportedEventType();
    void process(InboxEvent inboxEvent);
}
