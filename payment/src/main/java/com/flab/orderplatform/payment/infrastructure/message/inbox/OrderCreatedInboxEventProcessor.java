package com.flab.orderplatform.payment.infrastructure.message.inbox;

import com.flab.orderplatform.payment.application.PaymentFacade;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.common.JsonUtils;
import com.flab.orderplatform.payment.domain.InboxEvent;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED;

@Component
@RequiredArgsConstructor
public class OrderCreatedInboxEventProcessor implements InboxEventProcessor {

    private final PaymentFacade paymentFacade;

    @Override
    public String supportedEventType() {
        return ORDER_CREATED;
    }

    @Override
    public void process(InboxEvent inboxEvent) {
        var event = JsonUtils.fromJson(inboxEvent.getPayload(), OrderCreatedPayload.class);
        var command = PaymentCreateCommand.builder()
                .orderNumber(event.orderNumber())
                .buyerId(event.buyerId())
                .amount(event.totalAmount())
                .build();
        paymentFacade.pay(command);
    }
}
