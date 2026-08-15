package com.flab.orderplatform.inventory.infrastructure.message.inbox;

import com.flab.orderplatform.inventory.application.InventoryFacade;
import com.flab.orderplatform.shared.event.OrderPaidPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_PAID;

@Component
@RequiredArgsConstructor
public class OrderPaidEventProcessor implements InventoryInboxEventProcessor {

    private final InventoryFacade inventoryFacade;

    @Override
    public String supportedEventType() {
        return ORDER_PAID;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), OrderPaidPayload.class);
        inventoryFacade.decreaseStock(event);
    }
}
