package com.flab.orderplatform.order.infrastructure.message.outbox;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.shared.message.MessageProducer;
import com.flab.orderplatform.shared.outbox.OutboxEventRepository;
import com.flab.orderplatform.shared.outbox.OutboxEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderOutboxEventService extends OutboxEventService {

    public OrderOutboxEventService(@Qualifier("orderOutboxEventRepository") OutboxEventRepository outboxEventRepository,
                                   MessageProducer messageProducer,
                                   @Qualifier("orderOutboxStatusUpdateExecutor") ThreadPoolTaskExecutor outboxStatusUpdateExecutor) {
        super(outboxEventRepository, messageProducer, outboxStatusUpdateExecutor);
    }

    @Override
    @OrderTransactional
    public List<Long> bulkDeletePublishedEvents() {
        return super.bulkDeletePublishedEvents();
    }
}
