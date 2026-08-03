package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.DomainEvent;
import com.flab.orderplatform.order.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

/**
 * 도메인 이벤트 메시지 publisher
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventMessagePublisher {

    private final MessageProducer messageProducer;
    private final OutboxEventRepository outboxEventRepository;
    private final ThreadPoolTaskExecutor outboxStatusUpdateExecutor;

    /**
     * 아웃박스의 이벤트를 발행합니다.
     */
    @TransactionalEventListener(phase = BEFORE_COMMIT)
    public void saveToOutbox(DomainEvent event) {
        outboxEventRepository.save(OutboxEvent.create(event));
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void publishToKafka(DomainEvent event) {
        var searchedEvent = outboxEventRepository.findByEventId(event.getEventId());
        if (searchedEvent.isEmpty()) {
            log.error("이벤트가 아웃박스 테이블에 없습니다. (eventId: {}) 스케줄러로 복구 예정입니다.", event.getEventId());
        }
        var outboxEvent = searchedEvent.get();

        var future = messageProducer.sendMessage(
                outboxEvent.getTopic(),
                outboxEvent.getAggregateId(),
                outboxEvent.getPayload(),
                outboxEvent.toMessageHeaders());

        future.whenCompleteAsync((result, e) -> {
            try {
                updateOutboxStatus(outboxEvent, e);
            } catch (Exception exception) {
                log.error("outbox 상태 갱신 실패 (outboxEventId={})", outboxEvent.getId(), exception);
            }
        }, outboxStatusUpdateExecutor);
    }

    private void updateOutboxStatus(OutboxEvent outboxEvent, Throwable e) {
        if (e == null) {
            outboxEventRepository.save(outboxEvent.complete());
            return;
        }
        log.error("outbox 이벤트 발행 실패 (outboxEventId={}, topic={})", outboxEvent.getId(), outboxEvent.getTopic(), e);
        outboxEventRepository.save(outboxEvent.fail());
    }
}
