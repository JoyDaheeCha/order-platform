package com.flab.orderplatform.order.infrastructure.config;

import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OrderDomainConfig {

    @Bean
    OrderNumberGenerator orderNumberGenerator(Clock clock) {
        return new OrderNumberGenerator(clock);
    }
}
