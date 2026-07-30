package com.flab.orderplatform.payment.infrastructure.message;

import com.flab.orderplatform.payment.application.PaymentFacade;
import com.flab.orderplatform.payment.application.annotation.Inbox;
import com.flab.orderplatform.payment.application.command.PaymentCreateCommand;
import com.flab.orderplatform.payment.common.JsonUtils;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDomainEventListener {

    private final PaymentFacade paymentFacade;

    @Inbox
    @KafkaListener(topics = ORDER_CREATED_TOPIC, groupId = "payment")
    public void handle(ConsumerRecord<String, String> payload) {
        log.debug("[OrderCreatedEvent] payload: {}", payload.value());
        var event = JsonUtils.fromJson(payload.value(), OrderCreatedPayload.class);

        var command = PaymentCreateCommand.builder()
                .orderNumber(event.orderNumber())
                .buyerId(event.buyerId())
                .amount(event.totalAmount())
                .build();

        paymentFacade.pay(command);
    }
}
