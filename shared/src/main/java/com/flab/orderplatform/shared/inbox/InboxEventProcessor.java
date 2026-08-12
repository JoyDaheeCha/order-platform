package com.flab.orderplatform.shared.inbox;

public interface InboxEventProcessor {
    String supportedEventType();
    void process(InboxEvent inboxEvent);
}
