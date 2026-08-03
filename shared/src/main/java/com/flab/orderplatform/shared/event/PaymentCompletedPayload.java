package com.flab.orderplatform.shared.event;

import static com.flab.orderplatform.shared.event.EventConstants.PAYMENT_COMPLETED;
import static com.flab.orderplatform.shared.event.EventConstants.PAYMENT_COMPLETED_TOPIC;

public record PaymentCompletedPayload(
        String orderNumber,
        String pgTid,
        Long amount
) implements EventContract{
    @Override
    public String eventType() {
        return PAYMENT_COMPLETED;
    }

    @Override
    public String topic() {
        return PAYMENT_COMPLETED_TOPIC;
    }
}
