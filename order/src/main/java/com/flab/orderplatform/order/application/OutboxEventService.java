package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.OutboxEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {
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
        var future = messageProducer.sendMessage(event.getTopic(), event.getAggregateId(), event.getPayload());
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

    /**
     * 발행 완료 데이터 중 현재로부터 7일이 경과한 데이터를 제거한다.
     * - outbox 테이블에 더 이상 사용하지 않는 데이터가 쌓이는것을 방지합니다.
     */
    @Transactional
    public List<Long> bulkDeletePublishedEvents() {
        var threshold = LocalDateTime.now().minusDays(7);

        var idsToDelete = outboxEventRepository.findIdByStatusAndCreatedAtBefore(PUBLISHED, threshold, DEFAULT_PAGE_REQUEST);

        // 더 이상 삭제할 데이터가 없다면 return
        if (idsToDelete.isEmpty()) {
            return Collections.emptyList();
        }
        outboxEventRepository.deleteByIdsInBulk(idsToDelete);
        log.info("outbox 발행완료 이벤트 정리 완료 (총 {}건)", idsToDelete.size());
        return idsToDelete;
    }
}
