package com.flab.orderplatform.inventory.domain.event;

import com.flab.orderplatform.shared.domain.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

/**
 * 재고 선점에 실패하였다
 */
@Getter
public class InventoryReservationFailedEvent extends DomainEvent {
    private final String orderNumber;

    @Builder
    protected InventoryReservationFailedEvent(String orderNumber, LocalDateTime occurredOn) {
        super(orderNumber, occurredOn);
        this.orderNumber = orderNumber;
    }

    public String getAction() {
        return INVENTORY_RESERVATION_FAILED;
    }

    public String getTopic() {
        return INVENTORY_RESERVATION_FAILED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_INVENTORY;
    }
}
