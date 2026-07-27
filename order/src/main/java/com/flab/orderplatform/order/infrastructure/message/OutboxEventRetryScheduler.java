package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.OutboxEventRetryService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * 아웃박스 패턴에서 이벤트 발행에 실패한 경우, 재실행하는 스케줄러
 */
@Component
@RequiredArgsConstructor
public class OutboxEventRetryScheduler {

    private final OutboxEventRetryService outboxEventRetryService;

    /**
     * 1분마다 발행 실패한 메시지를 조사하여 제거한다
     * - lockAtMostFor < 스케줄 주기(fixedDelay) 로 설정하여 다음 스케줄 주기에 바로 복구처리되도록 설정한다.
     */
    @Scheduled(fixedDelay = 1, timeUnit = MINUTES)
    @SchedulerLock(name = "publishFailedOutboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "50s")
    void publishFailedOutboxEvents() {
        outboxEventRetryService.publishFailedOutboxEvents();
    }

    // TODO: created 상태인데, kafka 에서 발행하고 ack를 주기전 application이 죽은 경우, published로 바뀌지 않을 수 있다. 해당 경우에 대비해 재발행 로직 추가
}
