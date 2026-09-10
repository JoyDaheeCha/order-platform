package com.flab.orderplatform.order.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

// TODO 이벤트 완성하기. 결제 인박스 패턴 유지를 위해 미리 만들어둠.
@Getter
public class OrderPaymentPreparedEvent extends OrderOutboxEvent {

    private final String orderNumber;

    @Builder
    protected OrderPaymentPreparedEvent(String aggregateId,
                                        LocalDateTime occurredOn,
                                        String orderNumber) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
    }


    @Override
    public String getAction() {
        return ORDER_PAYMENT_PREPARED;
    }

    @Override
    public String getTopic() {
        return ORDER_PAYMENT_PREPARED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_ORDER;
    }
}
