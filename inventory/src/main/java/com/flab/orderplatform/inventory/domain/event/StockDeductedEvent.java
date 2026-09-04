package com.flab.orderplatform.inventory.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Getter
public class StockDeductedEvent extends InventoryOutboxEvent {
    private final String orderNumber;

    @Builder
    protected StockDeductedEvent(String orderNumber, LocalDateTime occurredOn) {
        super(orderNumber, occurredOn);
        this.orderNumber = orderNumber;
    }

    @Override
    public String getAction() {
        return STOCK_DEDUCTED;
    }

    @Override
    public String getTopic() {
        return STOCK_DEDUCTED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_INVENTORY;
    }
}
