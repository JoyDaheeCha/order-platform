package com.flab.orderplatform.shared.event;

import static com.flab.orderplatform.shared.event.EventConstants.PAYMENT_FAILED;
import static com.flab.orderplatform.shared.event.EventConstants.PAYMENT_FAILED_TOPIC;

/**
 * 결제가 실패하였다 이벤트 페이로드
 */
public record PaymentFailedPayload(
        String orderNumber
) implements EventContract {

    @Override
    public String eventType() {
        return PAYMENT_FAILED;
    }

    @Override
    public String topic() {
        return PAYMENT_FAILED_TOPIC;
    }
}
