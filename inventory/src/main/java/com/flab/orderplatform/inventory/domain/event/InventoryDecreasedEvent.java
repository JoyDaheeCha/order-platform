package com.flab.orderplatform.inventory.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

/**
 * 재고가 감소되었다
 */
@Getter
public class InventoryDecreasedEvent extends InventoryOutboxEvent {
    private final String orderNumber;

    @Builder
    protected InventoryDecreasedEvent(String orderNumber, LocalDateTime occurredOn) {
        super(orderNumber, occurredOn);
        this.orderNumber = orderNumber;
    }

    @Override
    public String getAction() {
        return INVENTORY_DECREASED;
    }

    @Override
    public String getTopic() {
        return INVENTORY_DECREASED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_INVENTORY;
    }
}
