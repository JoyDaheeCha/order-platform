package com.flab.orderplatform.inventory.infrastructure.message;

import com.flab.orderplatform.shared.inbox.Inbox;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.flab.orderplatform.shared.event.EventConstants.*;

@Slf4j
@Component
public class InventoryDomainEventListener {

    @Inbox(AGGREGATE_INVENTORY)
    @KafkaListener(topics = {
            ORDER_CREATED_TOPIC,
            ORDER_PAID_TOPIC
    }, groupId = "inventory")
    public void handle(ConsumerRecord<String, String> payload) {
        log.debug("[{}] payload: {}", payload.topic(), payload.value());
        // InboxAspect 가 인박스 저장 담당. 로직처리는 InventoryInboxEventScheduler 에 있다.
    }
}
