package com.flab.orderplatform.inventory.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

/**
 * 재고를 선점하였다
 */
@Getter
public class InventoryReservedEvent extends InventoryOutboxEvent {
    private final String orderNumber;

    @Builder
    protected InventoryReservedEvent(String orderNumber, LocalDateTime occurredOn) {
        super(orderNumber, occurredOn);
        this.orderNumber = orderNumber;
    }

    @Override
    public String getAction() {
        return INVENTORY_RESERVED;
    }

    @Override
    public String getTopic() {
        return INVENTORY_RESERVED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_INVENTORY;
    }
}
