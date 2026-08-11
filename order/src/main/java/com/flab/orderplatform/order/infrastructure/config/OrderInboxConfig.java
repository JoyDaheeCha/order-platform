package com.flab.orderplatform.order.infrastructure.config;

import com.flab.orderplatform.order.infrastructure.message.inbox.OrderInboxEventProcessor;
import com.flab.orderplatform.order.infrastructure.persistence.OrderInboxEventJpaRepository;
import com.flab.orderplatform.shared.inbox.ContextInbox;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.AGGREGATE_ORDER;

@Configuration
public class OrderInboxConfig {

    @Bean
    ContextInbox orderInbox(
            OrderInboxEventJpaRepository jpaRepository,
            @Qualifier("orderTransactionManager") PlatformTransactionManager transactionManager,
            List<OrderInboxEventProcessor> processors
    ) {
        return new ContextInbox(AGGREGATE_ORDER, jpaRepository, transactionManager, processors);
    }
}
