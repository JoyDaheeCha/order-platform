package com.flab.orderplatform.payment.infrastructure.message.inbox;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtLeastFor = "PT1S", defaultLockAtMostFor = "PT30S")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(RedissonConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }

}
