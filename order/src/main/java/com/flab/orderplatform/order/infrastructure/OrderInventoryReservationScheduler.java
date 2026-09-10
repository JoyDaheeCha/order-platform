package com.flab.orderplatform.order.infrastructure;

import com.flab.orderplatform.order.application.OrderPayFacade;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * 재고 선점 해제 스케줄러
 */
@RequiredArgsConstructor
@Component
public class OrderInventoryReservationScheduler {

    private final OrderPayFacade orderPayFacade;

    @Scheduled(fixedDelay = 1, timeUnit = MINUTES)
    @SchedulerLock(name = "orderInventoryReservationLock", lockAtLeastFor = "10s", lockAtMostFor = "50s")
    void releaseReservation() {
        orderPayFacade.releaseReservation();
    }

}
