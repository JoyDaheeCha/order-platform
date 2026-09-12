package com.flab.orderplatform.inventory.infrastructure.message.inbox;

import com.flab.orderplatform.inventory.application.InventoryFacade;
import com.flab.orderplatform.shared.event.OrderCanceledPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CANCELLED;

@Component
@RequiredArgsConstructor
public class OrderCanceledEventProcessor implements InventoryInboxEventProcessor {

    private final InventoryFacade inventoryFacade;

    @Override
    public String supportedEventType() {
        return ORDER_CANCELLED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), OrderCanceledPayload.class);
        inventoryFacade.restoreReservedInventory(event);
    }
}
