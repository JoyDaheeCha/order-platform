package com.flab.orderplatform.payment.infrastructure.message;

import com.flab.orderplatform.payment.application.PaymentCommandHandler;
import com.flab.orderplatform.payment.common.JsonUtils;
import com.flab.orderplatform.payment.infrastructure.message.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentDomainEventListener {

    PaymentCommandHandler paymentCommandHandler;
    // TODO: 토픽 공통화
    @KafkaListener(topics = "MSG-ORDER-CREATED")
    public void handle(OrderCreatedEvent paylaod) {
        log.debug("[OrderCreatedEvent] paylaod: {}", JsonUtils.toJson(paylaod));
        paymentCommandHandler.handle(paylaod.toCommand());
    }
}
