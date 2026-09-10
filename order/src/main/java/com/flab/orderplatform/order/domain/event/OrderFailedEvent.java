package com.flab.orderplatform.order.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

/**
 * 주문에 실패하였다 이벤트
 */
@Getter
public class OrderFailedEvent extends OrderOutboxEvent {

    private final String orderNumber;

    @Builder
    protected OrderFailedEvent(String aggregateId,
                               LocalDateTime occurredOn,
                               String orderNumber) {
        super(aggregateId, occurredOn);
        this.orderNumber = orderNumber;
    }


    @Override
    public String getAction() {
        return ORDER_FAILED;
    }

    @Override
    public String getTopic() {
        return ORDER_FAILED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_ORDER;
    }

    @Builder
    public record OrderItemDto(
            String productCode,
            Integer quantity
    ) {
    }
}
