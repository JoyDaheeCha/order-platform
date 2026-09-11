package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.shared.inbox.Inbox;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Slf4j
@Component
public class OrderDomainEventListener {

    @Inbox(AGGREGATE_ORDER)
    @KafkaListener(topics = {
            INVENTORY_RESERVED_TOPIC,
            INVENTORY_RESERVATION_FAILED_TOPIC,
            PAYMENT_COMPLETED_TOPIC,
            PAYMENT_FAILED_TOPIC
    }, groupId = "order")
    public void handle(ConsumerRecord<String, String> payload) {
        log.debug("[{}] payload: {}", payload.topic(), payload.value());
        // InboxAspect 가 인박스 저장 담당. 로직처리는 OrderInboxEventScheduler 에 있다.
    }
}
