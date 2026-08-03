package com.flab.orderplatform.payment.infrastructure.message;

import com.flab.orderplatform.payment.application.annotation.Inbox;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.ORDER_CREATED_TOPIC;

@Slf4j
@Component
public class PaymentDomainEventListener {

    @Inbox
    @KafkaListener(topics = ORDER_CREATED_TOPIC, groupId = "payment")
    public void handle(ConsumerRecord<String, String> payload) {
        log.debug("[OrderCreatedEvent] payload: {}", payload.value());
        // InboxAspect 가 인박스 저장 담당. 로직처리는 InboxEventScheduler 에 있다.
    }
}
