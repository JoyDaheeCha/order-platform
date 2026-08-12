package com.flab.orderplatform.payment.infrastructure.message.inbox;

import com.flab.orderplatform.shared.inbox.ContextInbox;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.concurrent.TimeUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class PaymentInboxEventScheduler {

    @Qualifier("paymentInbox")
    private final ContextInbox inbox;

    @Scheduled(fixedDelay = 1, timeUnit = SECONDS)
    @SchedulerLock(name = "paymentProcessInboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "50s")
    void processInboxEvents() {
        inbox.processInboxEvents();
    }
}
