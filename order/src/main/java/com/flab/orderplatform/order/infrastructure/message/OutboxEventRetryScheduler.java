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

    /**
     * 시스템 장애로 발행 처리되지 않은 이벤트에 대해 재발행한다.
     */
    @Scheduled(fixedDelay = 10, timeUnit = MINUTES)
    @SchedulerLock(name = "publishNeverTriedOutboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "9m")
    void publishNeverTriedOutboxEvents() {
        outboxEventRetryService.publishNeverTriedOutboxEvents();
    }
}
