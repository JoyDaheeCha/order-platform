package com.flab.orderplatform.inventory.infrastructure.config;

import com.flab.orderplatform.inventory.infrastructure.message.inbox.InventoryInboxEventProcessor;
import com.flab.orderplatform.inventory.infrastructure.persistence.InventoryInboxEventJpaRepository;
import com.flab.orderplatform.shared.inbox.ContextInbox;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.AGGREGATE_INVENTORY;

@Configuration
public class InventoryInboxConfig {
    @Bean
    ContextInbox inventoryInbox(
            InventoryInboxEventJpaRepository jpaRepository,
            @Qualifier("inventoryTransactionManager") PlatformTransactionManager transactionManager,
            List<InventoryInboxEventProcessor> processors
    ) {
        return new ContextInbox(AGGREGATE_INVENTORY, jpaRepository, transactionManager, processors);
    }
}
