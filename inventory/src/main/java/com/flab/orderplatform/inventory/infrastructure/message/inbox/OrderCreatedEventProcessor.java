package com.flab.orderplatform.inventory.infrastructure.message.inbox;

import com.flab.orderplatform.inventory.application.InventoryFacade;
import com.flab.orderplatform.inventory.common.BusinessException;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED;

@Slf4j
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
        try {
            inventoryFacade.reserveInventory(event);
        } catch (BusinessException e) {
            // 재고 선점 하나라도 실패하면, 주문 하위 모든 상품에 대해 재고 선점 실패 처리한다.
            log.warn("재고 선점 실패 (orderNumber={})", event.orderNumber(), e);
            inventoryFacade.publishReservationFailed(event.orderNumber());
        }
    }
}
