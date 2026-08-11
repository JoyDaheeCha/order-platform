package com.flab.orderplatform.shared.outbox;

import com.flab.orderplatform.shared.message.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.flab.orderplatform.shared.outbox.OutboxEventStatus.*;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {
    /**
     * 아웃박스 이벤트중 발행되어야할 이벤트의 상태값
     */
    private static final List<OutboxEventStatus> OUTBOX_EVENT_STATUSES_TO_PUBLISH = List.of(CREATED, FAILED);
    /**
     * 아웃박스 테이블 조회시 500건씩 잘라서 처리 (오래된 데이터부터 처리)
     */
    private static final PageRequest DEFAULT_PAGE_REQUEST = PageRequest.of(0, 500, Sort.by("id").ascending());

    private final OutboxEventRepository outboxEventRepository;
    private final MessageProducer messageProducer;
    private final ThreadPoolTaskExecutor outboxStatusUpdateExecutor;

    /**
     * outbox 테이블에서 생성/발행실패 된 경우 발행
     */
    public void publishOutboxEvents() {
        var events = outboxEventRepository.findEventByStatusIn(OUTBOX_EVENT_STATUSES_TO_PUBLISH, DEFAULT_PAGE_REQUEST);
        events.forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        var header = event.toMessageHeaders();
        var future = messageProducer.sendMessage(event.getTopic(), event.getAggregateId(), event.getPayload(), header);
        future.whenCompleteAsync((result, e) -> {
            updateOutboxStatus(event, e);
        }, outboxStatusUpdateExecutor);
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
