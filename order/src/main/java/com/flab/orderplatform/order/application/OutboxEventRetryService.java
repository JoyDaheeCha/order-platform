package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventRetryService {
    private final OutboxEventRepository outboxEventRepository;
    private final MessageProducer messageProducer;

    public void publishFailedOutboxEvents() {
        var events = outboxEventRepository.findFailedEvents();
        events.forEach(this::retryPublish);
    }

    private void retryPublish(OutboxEvent event) {
        var future = messageProducer.sendMessage(event.getTopic(), event.getPayload());
        future.whenComplete((result, e) -> {
            updateOutboxStatus(event, e);
        });
    }

    private void updateOutboxStatus(OutboxEvent event, Throwable e) {
        try {
            if (e == null) {
                outboxEventRepository.save(event.complete());
                return;
            }
            log.error("outbox 이벤트 재발행 실패 (outboxEventId={}, topic={})", event.getId(), event.getTopic(), e);
            outboxEventRepository.save(event.fail());
        } catch (Exception ex) {
            log.error("outbox 상태 갱신 실패 (outboxEventId={})", event.getId(), ex);
        }
    }
}
