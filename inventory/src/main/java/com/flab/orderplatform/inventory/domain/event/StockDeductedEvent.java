package com.flab.orderplatform.inventory.domain.event;

import com.flab.orderplatform.shared.domain.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Getter
public class StockDeductedEvent extends DomainEvent {
    private final String orderNumber;

    @Builder
    protected StockDeductedEvent(String orderNumber, LocalDateTime occurredOn) {
        super(orderNumber, occurredOn);
        this.orderNumber = orderNumber;
    }

    public String getAction() {
        return STOCK_DEDUCTED;
    }

    public String getTopic() {
        return STOCK_DEDUCTED_TOPIC;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_INVENTORY;
    }
}
