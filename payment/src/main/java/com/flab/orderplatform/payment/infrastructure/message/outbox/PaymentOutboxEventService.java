package com.flab.orderplatform.payment.infrastructure.message.outbox;

import com.flab.orderplatform.payment.application.annotation.PaymentTransactional;
import com.flab.orderplatform.shared.message.MessageProducer;
import com.flab.orderplatform.shared.outbox.OutboxEventRepository;
import com.flab.orderplatform.shared.outbox.OutboxEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentOutboxEventService extends OutboxEventService {

    public PaymentOutboxEventService(@Qualifier("paymentOutboxEventRepository") OutboxEventRepository outboxEventRepository,
                                     MessageProducer messageProducer,
                                     @Qualifier("paymentOutboxStatusUpdateExecutor") ThreadPoolTaskExecutor outboxStatusUpdateExecutor) {
        super(outboxEventRepository, messageProducer, outboxStatusUpdateExecutor);
    }

    @Override
    @PaymentTransactional
    public List<Long> bulkDeletePublishedEvents() {
        return super.bulkDeletePublishedEvents();
    }
}
