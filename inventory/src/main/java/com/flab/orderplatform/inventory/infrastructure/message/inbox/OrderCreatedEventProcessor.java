package com.flab.orderplatform.inventory.infrastructure.message.inbox;

import com.flab.orderplatform.inventory.application.InventoryFacade;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED;

@Component
@RequiredArgsConstructor
public class OrderCreatedEventProcessor implements InventoryInboxEventProcessor {

    private final InventoryFacade inventoryFacade;

    @Override
    public String supportedEventType() {
        return ORDER_CREATED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), OrderCreatedPayload.class);
        inventoryFacade.reserveInventory(event);
    }
}
