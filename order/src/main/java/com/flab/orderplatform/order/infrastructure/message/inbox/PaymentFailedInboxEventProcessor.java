package com.flab.orderplatform.order.infrastructure.message.inbox;

import com.flab.orderplatform.order.application.OrderPayFacade;
import com.flab.orderplatform.shared.event.PaymentFailedPayload;
import com.flab.orderplatform.shared.inbox.InboxEvent;
import com.flab.orderplatform.shared.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.PAYMENT_FAILED;

@Component
@RequiredArgsConstructor
public class PaymentFailedInboxEventProcessor implements OrderInboxEventProcessor {

    private final OrderPayFacade orderPayFacade;

    @Override
    public String supportedEventType() {
        return PAYMENT_FAILED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), PaymentFailedPayload.class);
        orderPayFacade.failOrderByFailedPayment(event.orderNumber());
    }
}
