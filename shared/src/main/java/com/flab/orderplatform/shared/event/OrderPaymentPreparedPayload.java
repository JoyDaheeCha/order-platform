package com.flab.orderplatform.shared.event;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_PAYMENT_PREPARED;
import static com.flab.orderplatform.shared.event.EventConstants.ORDER_PAYMENT_PREPARED_TOPIC;

/**
 * "주문의 결제 준비가 완료되었다" 이벤트 페이로드
 */
public record OrderPaymentPreparedPayload(
        String orderNumber,
        Long buyerId,
        Long amount
) implements EventContract {

    @Override
    public String eventType() {
        return ORDER_PAYMENT_PREPARED;
    }

    @Override
    public String topic() {
        return ORDER_PAYMENT_PREPARED_TOPIC;
    }
}
