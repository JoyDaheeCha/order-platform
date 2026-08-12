package com.flab.orderplatform.inventory.infrastructure.message.outbox;

import com.flab.orderplatform.shared.outbox.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 아웃박스 스케줄러
 * 1) 정기적으로 이벤트 발행
 * 2) 발행완료 처리된 이벤트에 대해 주기적으로 제거
 */
@Component
@RequiredArgsConstructor
public class InventoryOutboxEventScheduler {

    @Qualifier("inventoryOutboxEventProcessor")
    private final OutboxEventProcessor processor;

    /**
     * 아웃박스 이벤트 발행한다.
     * - lockAtMostFor < 스케줄 주기(fixedDelay) 로 설정하여 다음 스케줄 주기에 바로 복구처리되도록 설정한다.
     */
    @Scheduled(fixedDelay = 1, timeUnit = SECONDS)
    @SchedulerLock(name = "inventoryPublishOutboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "50s")
    void publishOutboxEvents() {
        processor.publishOutboxEvents();
    }

    /**
     * 발행완료된 이벤트중, 생성일로부터 일주일이 경과한 경우 제거
     * - 매주 월요일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * MON")
    @SchedulerLock(name = "inventoryDeletePublishedOutboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "1h")
    void deletePublishedOutboxEvents() {
        processor.deletePublishedOutboxEvents();
    }
}
