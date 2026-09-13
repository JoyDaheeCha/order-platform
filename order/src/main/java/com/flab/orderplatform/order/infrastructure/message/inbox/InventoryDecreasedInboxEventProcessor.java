package com.flab.orderplatform.order.infrastructure.message.inbox;

import com.flab.orderplatform.order.application.OrderPayFacade;
import com.flab.orderplatform.shared.event.InventoryDecreasedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_DECREASED;

@Component
@RequiredArgsConstructor
public class InventoryDecreasedInboxEventProcessor implements OrderInboxEventProcessor {

    private final OrderPayFacade orderPayFacade;

    @Override
    public String supportedEventType() {
        return INVENTORY_DECREASED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), InventoryDecreasedPayload.class);
        orderPayFacade.confirmOrder(event.orderNumber());
    }
}
