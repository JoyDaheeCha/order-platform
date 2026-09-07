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
    private final LocalDateTime reservedAt;

    @Builder
    protected InventoryReservedEvent(String orderNumber, LocalDateTime occurredOn, LocalDateTime reservedAt) {
        super(orderNumber, occurredOn);
        this.orderNumber = orderNumber;
        this.reservedAt = reservedAt;
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
