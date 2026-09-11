package com.flab.orderplatform.order.infrastructure.message.inbox;

import com.flab.orderplatform.order.application.OrderPayFacade;
import com.flab.orderplatform.shared.event.InventoryReservationFailedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.INVENTORY_RESERVATION_FAILED;

/**
 * '재고 선점 실패하였다' 이벤트의 인박스 프로세서
 */
@Component
@RequiredArgsConstructor
public class InventoryReservationFailedInboxEventProcessor implements OrderInboxEventProcessor {

    private final OrderPayFacade orderPayFacade;

    @Override
    public String supportedEventType() {
        return INVENTORY_RESERVATION_FAILED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), InventoryReservationFailedPayload.class);
        orderPayFacade.failOrderByInventoryShortage(event.orderNumber());
    }
}
