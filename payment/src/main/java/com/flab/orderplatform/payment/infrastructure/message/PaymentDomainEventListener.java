package com.flab.orderplatform.payment.infrastructure.message;

import com.flab.orderplatform.payment.application.PaymentFacade;
import com.flab.orderplatform.payment.common.JsonUtils;
import com.flab.orderplatform.payment.infrastructure.message.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDomainEventListener {

    private final PaymentFacade paymentFacade;

    // TODO: 토픽 공통화
    @KafkaListener(topics = "MSG-ORDER-CREATED")
    public void handle(OrderCreatedEvent paylaod) {
        log.debug("[OrderCreatedEvent] paylaod: {}", JsonUtils.toJson(paylaod));
        paymentFacade.pay(paylaod.toCommand());
    }
}
