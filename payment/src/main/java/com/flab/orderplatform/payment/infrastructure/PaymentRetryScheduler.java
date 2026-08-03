package com.flab.orderplatform.payment.infrastructure;

import com.flab.orderplatform.payment.application.PaymentFacade;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 결제 재시도 스케줄러
 */
@RequiredArgsConstructor
@Service
public class PaymentRetryScheduler {

    private final PaymentFacade paymentFacade;

    @Scheduled(fixedDelay = 5, timeUnit = SECONDS)
    @SchedulerLock(name="retryPaymentsInProcess", lockAtLeastFor = "10s", lockAtMostFor = "290s")
    void retryPaymentsInProcess() {
        paymentFacade.retryPaymentInProcess();
    }
}
