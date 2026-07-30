package com.flab.orderplatform.payment.infrastructure.message;

import com.flab.orderplatform.payment.application.port.out.InboxEventRepository;
import com.flab.orderplatform.payment.domain.InboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 결제 시스템에서 인박스 패턴을 적용한 토픽에 대해 적용되는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInboxEventListener {

    private final InboxEventRepository inboxEventRepository;

    @KafkaListener(topics = {"MSG-ORDER-CREATED"})
    public void handle(@Headers MessageHeaders headers,
                       @Payload String payload) {
        log.debug("[InboxPayload Consumed] paylaod: {}", payload);

        var eventId = headers.get("eventId", String.class);
        var aggregateType = headers.get("aggregateType", String.class);
        var eventType = headers.get("eventType", String.class);

        var inboxEvent = InboxEvent.create(eventId, aggregateType, eventType, payload);
        inboxEventRepository.save(inboxEvent);
    }
}
