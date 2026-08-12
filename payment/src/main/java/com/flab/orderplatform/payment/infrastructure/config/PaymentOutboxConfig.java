package com.flab.orderplatform.payment.infrastructure.config;

import com.flab.orderplatform.payment.domain.PaymentOutboxEvent;
import com.flab.orderplatform.payment.infrastructure.persistence.PaymentOutboxEventJpaRepository;
import com.flab.orderplatform.shared.outbox.OutboxEventProcessor;
import com.flab.orderplatform.shared.outbox.OutboxEventRepository;
import com.flab.orderplatform.shared.outbox.OutboxEventRepositoryAdaptor;
import com.flab.orderplatform.shared.outbox.OutboxEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class PaymentOutboxConfig {

    @Bean(name = "paymentOutboxStatusUpdateExecutor")
    public ThreadPoolTaskExecutor paymentOutboxStatusUpdateExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("outbox-status-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy()); // 처리할 수 없을때 reject
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Bean
    OutboxEventRepository paymentOutboxEventRepository(PaymentOutboxEventJpaRepository jpaRepository) {
        return new OutboxEventRepositoryAdaptor(jpaRepository);
    }

    @Bean
    OutboxEventProcessor paymentOutboxEventProcessor(
            @Qualifier("paymentOutboxEventRepository") OutboxEventRepository repository,
            @Qualifier("paymentOutboxEventService") OutboxEventService outboxEventService) {
        return new OutboxEventProcessor(PaymentOutboxEvent.class, repository, outboxEventService);
    }
}
