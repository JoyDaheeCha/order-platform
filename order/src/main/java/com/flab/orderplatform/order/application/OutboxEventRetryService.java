package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.CREATED;
import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventRetryService {
    private final OutboxEventRepository outboxEventRepository;
    private final MessageProducer messageProducer;
    /**
     * 아웃박스 테이블 조회시 500건씩 잘라서 처리 (오래된 데이터부터 처리)
     */
    private final static PageRequest DEFAULT_PAGE_REQUEST = PageRequest.of(0, 500, Sort.by("id").ascending());

    /**
     * Outbox 테이블에서 생성후 10분이 지나도록 이벤트 발행 성공/실패 기록이 없을 경우, 재발행한다. (500건씩 처리)
     * - 예. Kafka에서 메시지를 발행했으나, outbox에서 상태값 변경을 하기전 애플리케이션이 죽었을 경우, CREATED 상태로 남는다.
     */
    public void publishNeverTriedOutboxEvents() {
        var threshold = LocalDateTime.now().minusMinutes(10);
        var events = outboxEventRepository.findEventsCreatedAndNeverExecuted(CREATED, threshold, DEFAULT_PAGE_REQUEST);
        events.forEach(this::retryPublish);
    }

    /**
     * outbox 테이블에서 발행 실패한 경우 재발행 (500건씩 처리)
     */
    public void publishFailedOutboxEvents() {
        var events = outboxEventRepository.findEventByStatus(FAILED, DEFAULT_PAGE_REQUEST);
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
