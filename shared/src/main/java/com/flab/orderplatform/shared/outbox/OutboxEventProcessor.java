package com.flab.orderplatform.shared.outbox;


import com.flab.orderplatform.shared.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

/**
 * 아웃박스 처리
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxEventProcessor {

    /**
     * 드레인 루프 상한 — 락 유효시간(lockAtMostFor)을 넘겨 두 노드가 동시 진입하는 것을 막는다.
     **/
    private static final int MAX_DRAIN_ROUNDS = 200;   // 200 × 500건 = 10만건/회

    private final Class<? extends DomainEvent> supportedEventType;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventService outboxEventService;

    /**
     * 아웃박스 테이블에 도메인 이벤트를 저장합니다
     */
    @TransactionalEventListener(phase = BEFORE_COMMIT)
    public void saveToOutbox(DomainEvent event) {
        if (!supports(event)) return;
        outboxEventRepository.save(OutboxEvent.create(event));
    }

    /**
     * 아웃박스 이벤트 발행
     */
    public void publishOutboxEvents() {
        outboxEventService.publishOutboxEvents();
    }

    /**
     * 발행완료된 이벤트중, 생성일로부터 일주일이 경과한 경우 제거
     */
    public void deletePublishedOutboxEvents() {
        var round = 0;
        for (; round < MAX_DRAIN_ROUNDS; round++) {
            var deletedIds = outboxEventService.bulkDeletePublishedEvents();
            if (deletedIds.isEmpty()) {
                break;
            }
        }
        if (round == MAX_DRAIN_ROUNDS) {
            log.warn("아웃박스 정리 상한({}회) 도달 — 다음 주기에 이어서 처리됩니다.", MAX_DRAIN_ROUNDS);
        }
    }

    private boolean supports(DomainEvent event) {
        return supportedEventType.isInstance(event);
    }
}
