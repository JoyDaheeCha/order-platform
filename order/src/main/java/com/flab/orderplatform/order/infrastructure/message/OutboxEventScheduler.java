package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.OutboxEventService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * 아웃박스 스케줄러
 * 1) 이벤트 발행에 실패한 경우, 재실행
 * 2) 시스템 장애로 발행완료처리되지 않은 경우, 재실행
 * 3) 발행완료 처리된 이벤트에 대해 주기적으로 제거
 */
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventService outboxEventService;

    /**
     * 1분마다 발행 실패한 메시지를 조사하여 제거한다
     * - lockAtMostFor < 스케줄 주기(fixedDelay) 로 설정하여 다음 스케줄 주기에 바로 복구처리되도록 설정한다.
     */
    @Scheduled(fixedDelay = 1, timeUnit = MINUTES)
    @SchedulerLock(name = "publishFailedOutboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "50s")
    void publishFailedOutboxEvents() {
        outboxEventService.publishFailedOutboxEvents();
    }

    /**
     * 시스템 장애로 발행 처리되지 않은 이벤트에 대해 재발행한다.
     */
    @Scheduled(fixedDelay = 10, timeUnit = MINUTES)
    @SchedulerLock(name = "publishNeverTriedOutboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "9m")
    void publishNeverTriedOutboxEvents() {
        outboxEventService.publishNeverTriedOutboxEvents();
    }

    /**
     * 발행완료된 이벤트중, 생성일로부터 일주일이 경과한 경우 제거
     * - 매주 월요일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * MON")
    @SchedulerLock(name = "deletePublishedEvents", lockAtLeastFor = "10s", lockAtMostFor = "1h")
    void deletePublishedEvents() {
        while (true) {
            var deletedIds = outboxEventService.bulkDeletePublishedEvents();
            if (deletedIds.isEmpty()) {
                break;
            }
        }
    }
}
