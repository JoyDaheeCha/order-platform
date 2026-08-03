package com.flab.orderplatform.order.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class OutboxExecutorConfig {

    @Bean(name="outboxStatusUpdateExecutor")
    public ThreadPoolTaskExecutor outboxStatusUpdateExecutor() {
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
}
