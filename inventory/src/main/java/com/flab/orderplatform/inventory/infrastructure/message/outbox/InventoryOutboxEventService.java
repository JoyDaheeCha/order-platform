package com.flab.orderplatform.inventory.infrastructure.message.outbox;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.shared.message.MessageProducer;
import com.flab.orderplatform.shared.outbox.OutboxEventRepository;
import com.flab.orderplatform.shared.outbox.OutboxEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryOutboxEventService extends OutboxEventService {

    public InventoryOutboxEventService(@Qualifier("inventoryOutboxEventRepository") OutboxEventRepository outboxEventRepository,
                                       MessageProducer messageProducer,
                                       @Qualifier("inventoryOutboxStatusUpdateExecutor") ThreadPoolTaskExecutor outboxStatusUpdateExecutor) {
        super(outboxEventRepository, messageProducer, outboxStatusUpdateExecutor);
    }

    @Override
    @InventoryTransactional
    public List<Long> bulkDeletePublishedEvents() {
        return super.bulkDeletePublishedEvents();
    }
}
