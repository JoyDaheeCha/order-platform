package com.flab.orderplatform.order.infrastructure.message.inbox;

import com.flab.orderplatform.order.application.OrderPayFacade;
import com.flab.orderplatform.shared.event.PaymentCompletedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.PAYMENT_COMPLETED;

@Component
@RequiredArgsConstructor
public class PaymentCompletedInboxEventProcessor implements OrderInboxEventProcessor {

    private final OrderPayFacade orderPayFacade;

    @Override
    public String supportedEventType() {
        return PAYMENT_COMPLETED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), PaymentCompletedPayload.class);
        orderPayFacade.pay(event.orderNumber());
    }
}
