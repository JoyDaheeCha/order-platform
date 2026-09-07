package com.flab.orderplatform.order.infrastructure.message.inbox;

import com.flab.orderplatform.order.application.OrderPayFacade;
import com.flab.orderplatform.shared.event.InventoryReservedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_RESERVED;

@Component
@RequiredArgsConstructor
public class InventoryReservedInboxEventProcessor implements OrderInboxEventProcessor {

    private final OrderPayFacade orderPayFacade;

    @Override
    public String supportedEventType() {
        return INVENTORY_RESERVED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), InventoryReservedPayload.class);
        orderPayFacade.preparePayment(event.orderNumber(), event.reservedAt());
    }
}
