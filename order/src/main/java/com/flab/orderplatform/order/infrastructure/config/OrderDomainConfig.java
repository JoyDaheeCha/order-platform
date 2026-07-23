package com.flab.orderplatform.order.infrastructure.config;

import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "orderAuditorAware")
public class OrderDomainConfig {

    @Bean
    OrderNumberGenerator orderNumberGenerator() {
        return new OrderNumberGenerator();
    }

    // TODO: 인증/인가 도입되면 SecurityContext 에서 현재 사용자를 가져오도록 교체
    @Bean
    AuditorAware<String> orderAuditorAware() {
        return () -> Optional.of("system");
    }
}
